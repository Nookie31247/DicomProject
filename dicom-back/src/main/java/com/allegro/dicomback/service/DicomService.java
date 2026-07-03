package com.allegro.dicomback.service;

import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.Series;
import com.allegro.dicomback.entity.Study;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.PatientRepository;
import com.allegro.dicomback.repository.SeriesRepository;
import com.allegro.dicomback.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.allegro.dicomback.dto.DicomRequestDto.*;
import com.allegro.dicomback.dto.DicomResponseDto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DicomService {

    // 레포지토리 의존성 주입하기
    //    private final ImageRepository imageRepository;
    private final SeriesRepository seriesRepository;
    private final StudyRepository studyRepository;
    private final PatientRepository patientRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${orthanc.url:http://localhost:8042}")
    private String orthancUrl;

    // 단일 이미지 (.dcm) 다운로드
    //Get http://localhost:8080/api/dicom/images/download?image-key=1
//    public Resource downloadImage(Long imageKey) {
//        Image image = imageRepository.findById(imageKey)
//                .orElseThrow(() -> new BaseException(ErrorCode.IMAGE_NOT_FOUND));
//
//        if (image.getDelFlag() == 1) {
//            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND);
//        }
//
//        String url = orthancUrl + "/instances/" + image.getPath() + "/file";
//        byte[] fileBytes = restTemplate.getForObject(url, byte[].class);
//
//        if (fileBytes == null) {
//            throw new BaseException(ErrorCode.FILE_NOT_FOUND_ON_DISK);
//        }
//
//        return new ByteArrayResource(fileBytes);
//    }

    // 시리즈 전체 ZIP 다운로드 (Orthanc /archive)
    //GET http://localhost:8080/api/dicom/series/download?series-key=1
    public StreamingResponseBody downloadSeriesAsZip(Long seriesKey) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));

        // orthancSeriesId가 null이면 동기화가 안 된 상태이므로 미리 차단
        if (series.getOrthancSeriesId() == null) {
            log.warn("orthancSeriesId가 없습니다. seriesKey: {} (동기화 필요)", seriesKey);
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        String url = orthancUrl + "/series/" + series.getOrthancSeriesId() + "/archive";

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
        if (study.getOrthancStudyId() == null) {
            log.warn("orthancStudyId가 없습니다. studyKey: {} (동기화 필요)", studyKey);
            throw new BaseException(ErrorCode.STUDY_NOT_SYNCED);
        }

        String url = orthancUrl + "/studies/" + study.getOrthancStudyId() + "/archive";

        //스트리밍 프록시
        return outputStream -> {
            restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
                StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
                return null;
            });
        };
    }


    // ======================================== 이영무 추가 ======================================================

    public List<PatientDto> getPatients(String start, String end, String search) {
        List<Patient> patientList;
        List<PatientDto> patientDtoList = new ArrayList<>();
        LocalDateTime startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(3);
        LocalDateTime endDay = LocalDateTime.now();

        // 시작일, 종료일이 입력되지 않았을 때
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            // 모든 검색 결과를 다 전송하면 랙걸리니까, 기본값은 최근 3개월로 제한
            startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(3);
            endDay = LocalDateTime.now();
        }
        // 시작일과 종료일이 모두 입력되었을 때
        else {
            try {
                startDay = LocalDateTime.of(LocalDate.parse(start), LocalTime.MIN);
                endDay = LocalDateTime.of(LocalDate.parse(end), LocalTime.MAX);
            } catch (DateTimeParseException e) {
                // TODO 여기 커스텀 에러 추가하기
                System.out.println("날짜 형식이 안맞아요");
            }
        }

        // 검색어가 있을 때
        if(StringUtils.hasText(search)) {
            // 여기서는 시작일, 종료일, 검색어 3가지 모두를 가지고 검색한다.
            patientList = patientRepository.findByNameContainsAndRecentStudyBetween(search, startDay, endDay);
        }
        // 검색어가 없을 때
        else {
            // 여기서는 시작일과 종료일만 가지고 검색한다.
            patientList = patientRepository.findByRecentStudyBetween(startDay, endDay);
        }

        for(Patient p : patientList) {
            patientDtoList.add(new PatientDto(
                    p.getId(),
                    p.getName(),
                    p.getBirth(),
                    p.getSex(),
                    p.getRecentStudy(),
                    p.getStudyCount(),
                    p.getHiddenFlag() == 1
            ));
        }

        return patientDtoList;
    }
}