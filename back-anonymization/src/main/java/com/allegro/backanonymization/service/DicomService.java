package com.allegro.backanonymization.service;

import com.allegro.backanonymization.dto.DicomRequestDto.BatchDownloadDto;
import com.allegro.backanonymization.dto.DicomResponseDto.InstanceInfoDto;
import com.allegro.backanonymization.dto.DicomResponseDto.SeriesDto;
import com.allegro.backanonymization.dto.DicomResponseDto.StudyDto;
import com.allegro.backanonymization.entity.Series;
import com.allegro.backanonymization.entity.Study;
import com.allegro.backanonymization.exception.BaseException;
import com.allegro.backanonymization.exception.ErrorCode;
import com.allegro.backanonymization.repository.SeriesRepository;
import com.allegro.backanonymization.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DICOM 작업을 위한 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DicomService {

    private final SeriesRepository seriesRepository;
    private final StudyRepository studyRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${orthanc.url:http://localhost:8043}")
    private String orthancUrl;

    /**
     * 연구 데이터를 가져옵니다.
     *
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param search 검색어
     * @return 연구 DTO 목록
     */
    public List<StudyDto> getStudiesData(String start, String end, String search) {
        DateRange range = resolveDateRange(start, end);
        List<Study> studyList = studyRepository.findStudiesForResearch(range.start(), range.end(), search);

        if (studyList.isEmpty()) {
            return List.of();
        }

        List<Long> studyKeys = studyList.stream()
                .map(Study::getKey)
                .toList();

        Map<Long, SeriesRepository.SeriesAndImagesCount> countMap =
                seriesRepository.getSeriesAndImagesCount(studyKeys)
                        .stream()
                        .collect(Collectors.toMap(
                                SeriesRepository.SeriesAndImagesCount::getStudyKey,
                                Function.identity()
                        ));

        return studyList.stream()
                .map(study -> {
                    SeriesRepository.SeriesAndImagesCount count = countMap.get(study.getKey());
                    Long seriesNum = count == null ? 0L : count.getSeriesNum();
                    Long imagesNum = count == null ? 0L : count.getImagesNum();
                    LocalDateTime displayDateTime = study.getStudyDate() != null
                            ? study.getStudyDate().atStartOfDay()
                            : study.getCreatedAt();

                    return new StudyDto(
                            study.getKey(),
                            study.getDescription(),
                            displayDateTime,
                            seriesNum,
                            imagesNum,
                            true,
                            false
                    );
                })
                .toList();
    }

    /**
     * 연구에 대한 시리즈 데이터를 가져옵니다.
     *
     * @param studyKey 연구 키
     * @return 시리즈 DTO 목록
     */
    public List<SeriesDto> getSeriesData(Long studyKey) {
        if (!studyRepository.existsById(studyKey)) {
            throw new BaseException(ErrorCode.STUDY_NOT_FOUND);
        }

        return seriesRepository.findSeriesByStudyKey_Key(studyKey)
                .stream()
                .map(series -> new SeriesDto(
                        series.getKey(),
                        series.getSeriesNum(),
                        series.getCreatedAt(),
                        series.getSeriesNum(),
                        series.getBodyPart(),
                        series.getHiddenFlag() != null && series.getHiddenFlag()
                ))
                .toList();
    }

    /**
     * 시리즈를 ZIP으로 다운로드합니다.
     *
     * @param seriesKey 시리즈 키
     * @return 스트리밍 응답 본문
     */
    public StreamingResponseBody downloadSeriesAsZip(Long seriesKey) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));

        if (series.getOrthancId() == null) {
            log.warn("orthancSeriesId is missing. seriesKey: {}", seriesKey);
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        String url = orthancUrl + "/series/" + series.getOrthancId() + "/archive";

        return outputStream -> restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
            return null;
        });
    }

    /**
     * 연구를 ZIP으로 다운로드합니다.
     *
     * @param studyKey 연구 키
     * @return 스트리밍 응답 본문
     */
    public StreamingResponseBody downloadStudyAsZip(Long studyKey) {
        Study study = studyRepository.findById(studyKey)
                .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));

        if (study.getOrthancId() == null) {
            log.warn("orthancStudyId is missing. studyKey: {}", studyKey);
            throw new BaseException(ErrorCode.STUDY_NOT_SYNCED);
        }

        String url = orthancUrl + "/studies/" + study.getOrthancId() + "/archive";

        return outputStream -> restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
            return null;
        });
    }

    /**
     * 연구/시리즈의 배치를 ZIP으로 다운로드합니다.
     *
     * @param request 배치 다운로드 요청
     * @return 스트리밍 응답 본문
     */
    public StreamingResponseBody downloadBatchAsZip(BatchDownloadDto request) {
        List<String> orthancIds = new ArrayList<>();

        if (request.studyKeys() != null) {
            for (Long studyKey : request.studyKeys()) {
                Study study = studyRepository.findById(studyKey)
                        .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));
                if (study.getOrthancId() == null) {
                    log.warn("orthancStudyId is missing. studyKey: {}", studyKey);
                    throw new BaseException(ErrorCode.STUDY_NOT_SYNCED);
                }
                orthancIds.add(study.getOrthancId());
            }
        }

        if (request.seriesKeys() != null) {
            for (Long seriesKey : request.seriesKeys()) {
                Series series = seriesRepository.findById(seriesKey)
                        .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));
                if (series.getOrthancId() == null) {
                    log.warn("orthancSeriesId is missing. seriesKey: {}", seriesKey);
                    throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
                }
                orthancIds.add(series.getOrthancId());
            }
        }

        if (orthancIds.isEmpty()) {
            throw new BaseException(ErrorCode.EMPTY_DOWNLOAD_SELECTION);
        }

        String requestBody = "{\"Resources\":[" +
                orthancIds.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",")) +
                "]}";

        return outputStream -> restTemplate.execute(
                orthancUrl + "/tools/create-archive",
                HttpMethod.POST,
                clientHttpRequest -> {
                    clientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    clientHttpRequest.getBody().write(requestBody.getBytes(StandardCharsets.UTF_8));
                },
                clientHttpResponse -> {
                    StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
                    return null;
                }
        );
    }

    /**
     * 시리즈에 대한 인스턴스 데이터를 가져옵니다.
     *
     * @param seriesKey 시리즈 키
     * @return 인스턴스 DTO 목록
     */
    public List<InstanceInfoDto> getInstancesBySeries(Long seriesKey) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));

        if (series.getOrthancId() == null) {
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        return getOrthancInstances(series.getOrthancId()).stream()
                .sorted(Comparator.comparingInt(this::extractInstanceNumber))
                .map(instance -> new InstanceInfoDto(
                        (String) instance.get("ID"),
                        extractNumberOfFrames(instance)
                ))
                .toList();
    }

    /**
     * 특정 인스턴스 파일을 가져옵니다.
     *
     * @param seriesKey 시리즈 키
     * @param instanceId 인스턴스 ID
     * @return 스트리밍 응답 본문
     */
    public StreamingResponseBody getInstanceFile(Long seriesKey, String instanceId) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));

        if (series.getOrthancId() == null) {
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        List<String> validInstanceIds = getOrthancInstances(series.getOrthancId()).stream()
                .map(instance -> (String) instance.get("ID"))
                .toList();

        if (!validInstanceIds.contains(instanceId)) {
            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND);
        }

        String url = orthancUrl + "/instances/" + instanceId + "/file";

        return outputStream -> restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
            return null;
        });
    }

    private DateRange resolveDateRange(String start, String end) {
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            return new DateRange(
                    LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusYears(1),
                    LocalDateTime.now()
            );
        }

        try {
            return new DateRange(
                    LocalDateTime.of(LocalDate.parse(start), LocalTime.MIN),
                    LocalDateTime.of(LocalDate.parse(end), LocalTime.MAX)
            );
        } catch (DateTimeParseException e) {
            log.warn("Invalid research study date range. start={}, end={}", start, end);
            return new DateRange(
                    LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusYears(1),
                    LocalDateTime.now()
            );
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getOrthancInstances(String orthancSeriesId) {
        String url = orthancUrl + "/series/" + orthancSeriesId + "/instances?expand=true";
        List<Map<String, Object>> instances = restTemplate.getForObject(url, List.class);
        return instances == null ? Collections.emptyList() : instances;
    }

    @SuppressWarnings("unchecked")
    private int extractInstanceNumber(Map<String, Object> instance) {
        try {
            Map<String, Object> tags = (Map<String, Object>) instance.get("MainDicomTags");
            String num = (String) tags.get("InstanceNumber");
            return num != null ? Integer.parseInt(num.trim()) : Integer.MAX_VALUE;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    @SuppressWarnings("unchecked")
    private int extractNumberOfFrames(Map<String, Object> instance) {
        try {
            Map<String, Object> tags = (Map<String, Object>) instance.get("MainDicomTags");
            String num = (String) tags.get("NumberOfFrames");
            return num != null && !num.isBlank() ? Integer.parseInt(num.trim()) : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {}
}
