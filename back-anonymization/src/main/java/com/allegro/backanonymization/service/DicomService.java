package com.allegro.backanonymization.service;

import com.allegro.backanonymization.dto.AnonymizationRequestDto;
import com.allegro.backanonymization.entity.Series;
import com.allegro.backanonymization.entity.Study;
import com.allegro.backanonymization.exception.BaseException;
import com.allegro.backanonymization.exception.ErrorCode;
import com.allegro.backanonymization.repository.SeriesRepository;
import com.allegro.backanonymization.repository.StudyRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.allegro.backanonymization.dto.DicomResponseDto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DicomService {

    // 레포지토리 의존성 주입하기
    private final SeriesRepository seriesRepository;
    private final StudyRepository studyRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${orthanc.url:http://localhost:8042}")
    private String orthancUrl;

    private record OrthancStudyDetail(
        @JsonProperty("ID") String id,
        @JsonProperty("MainDicomTags") Map<String, String> mainDicomTags
    ) {}

//    public List<StudyDto> getStudiesData(String start, String end) {
//        List<Study> studyList;
//        LocalDateTime startDay;
//        LocalDateTime endDay;
//
//        // 시작 날짜(start)와 종료 날짜(end)가 입력되었을 때에는 검색 범위를 입력된 값으로 하고
//        // 입력되지 않았을 때에는 기본값인 최근으로부터 3개월치를 검색합니다.
//        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
//            startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(3);
//            endDay = LocalDateTime.now();
//        }
//        else {
//            try {
//                startDay = LocalDateTime.of(LocalDate.parse(start), LocalTime.MIN);
//                endDay = LocalDateTime.of(LocalDate.parse(end), LocalTime.MAX);
//            } catch (DateTimeParseException e) {
//                // 날짜 형식이 안맞으면 강제로 기본값으로 변경
//                startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(3);
//                endDay = LocalDateTime.now();
//                System.out.println("날짜 형식이 안맞아요");
//            }
//        }
//
//        studyList = studyRepository.findStudiesByCreatedAtBetween(startDay, endDay);
//
//        if (studyList.isEmpty()) {
//            return List.of();
//        }
//
//        // 시리즈와 이미지의 개수를 구하기 위해 스터디 키를 가져온다.
//        List<Long> studyKeys = studyList.stream()
//                .map(Study::getKey)
//                .toList();
//
//        // SeriesAndImagesCount는 스터디 키, 시리즈 개수, 이미지 개수로 이루어진 record 값인데
//        // 여기서 하나하나 for문 돌리면 연산이 많으니까 스터디 키를 Key로 하는 맵 타입을 만든다.
//        Map<Long, SeriesRepository.SeriesAndImagesCount> countMap =
//                seriesRepository.getSeriesAndImagesCount(studyKeys)
//                        .stream()
//                        .collect(Collectors.toMap(
//                                SeriesRepository.SeriesAndImagesCount::getStudyKey,
//                                Function.identity()
//                        ));
//
//        return studyList.stream()
//                .map(study -> {
//                    SeriesRepository.SeriesAndImagesCount count = countMap.get(study.getKey());
//
//                    Long seriesNum = count == null ? 0L : count.getSeriesNum();
//                    Long imagesNum = count == null ? 0L : count.getImagesNum();
//
//                    return new StudyDto(
//                            study.getKey(),
//                            study.getDescription(),
//                            study.getCreatedAt(),
//                            seriesNum,
//                            imagesNum,
//                            study.getAllowResearch(),
//                            study.getHiddenFlag()
//                    );
//                })
//                .toList();
//    }

    public List<SeriesDto> getSeriesData(Long studyKey) {
        List<Series> seriesList = seriesRepository.findSeriesByStudyKey_Key(studyKey);
        List<SeriesDto> seriesDtoList = new ArrayList<>();
        seriesList.forEach(s -> seriesDtoList.add(new SeriesDto(
                        s.getKey(),
                        s.getSeriesNum(),
                        s.getCreatedAt(),
                        s.getSeriesNum(),
                        s.getBodyPart(),
                        s.getHiddenFlag()
                ))
        );

        return seriesDtoList;

    }

    // 시리즈 전체 ZIP 다운로드 (Orthanc /archive)
    //GET http://localhost:8080/api/dicom/series/download?series-key=1
    public StreamingResponseBody downloadSeriesAsZip(Long seriesKey) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));

        // orthancSeriesId가 null이면 동기화가 안 된 상태이므로 미리 차단
        if (series.getOrthancId() == null) {
            log.warn("orthancSeriesId가 없습니다. seriesKey: {} (동기화 필요)", seriesKey);
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        String url = orthancUrl + "/series/" + series.getOrthancId() + "/archive";

        //스트리밍 프록시
        return outputStream -> {
            restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
                StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
                return null;
            });
        };
    }

    // 스터디 전체 ZIP 다운로드 (Orthanc /archiv)
    //GET http://localhost:8080/api/dicom/studies/download?study-key=1
    public StreamingResponseBody downloadStudyAsZip(Long studyKey) {
        Study study = studyRepository.findById(studyKey)
                .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));

        // orthancStudyId가 null이면 동기화가 안 된 상태이므로 미리 차단
        if (study.getOrthancId() == null) {
            log.warn("orthancStudyId가 없습니다. studyKey: {} (동기화 필요)", studyKey);
            throw new BaseException(ErrorCode.STUDY_NOT_SYNCED);
        }

        String url = orthancUrl + "/studies/" + study.getOrthancId() + "/archive";

        //스트리밍 프록시
        return outputStream -> {
            restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
                StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
                return null;
            });
        };
    }
}
