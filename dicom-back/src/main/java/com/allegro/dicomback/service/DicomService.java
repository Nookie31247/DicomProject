package com.allegro.dicomback.service;

import com.allegro.dicomback.dto.DicomResponseDto;
import com.allegro.dicomback.entity.Patient;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    // 픽셀 데이터가 없어 이미지 뷰어로 열 수 없는 modality (Presentation State, Structured Report, Key Object 등)
    //  DB에는 그대로 보관하되, 이미지 뷰어용 시리즈 목록에서는 제외한다.
    //  PR (Presentation State): 영상의 밝기, 대조도, 확대, 회전, 주석 등 화면에 보이는 출력 상태를 원본 파괴 없이 저장하는 기능
    //  SR (Structured Report): 영상 진단 결과, 측정 수치, 의사의 소견 등을 기계가 읽을 수 있는 표준화된 문서 형식으로 기록하는 기능
    //  KO (Key Object Selection): 수많은 영상 중 진단이나 연구에 중요한 핵심 영상만을 골라내어 따로 표시하고 관리하는 기능
    //  DOC (Document): 환자의 진료 기록, 소견서, 외부 보고서 등의 일반 문서 데이터를 DICOM 규격 내에 캡슐화하여 저장하는 기능
    //  AU (Audio): 수술 중 녹음된 음성 소견이나 심장 초음파의 도플러 혈류 소리 등 의료 영상과 관련된 오디오 데이터를 저장하는 기능
    //  REG (Registration): CT와 MRI 등 서로 다른 시점이나 다른 장비로 촬영한 영상을 정밀하게 겹쳐서 비교할 수 있도록 정렬하는 기능
    //  SEG (Segmentation): 의료 영상 인공지능(AI)이나 분석 소프트웨어가 장기, 종양 등 특정 관심 부위의 경계를 지정하고 분할하여 표시하는 기능
    private static final Set<String> NON_IMAGE_MODALITIES = Set.of("PR");


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

    // 프론트엔드 뷰어를 위한 시리즈 내 인스턴스(단면) ID 목록 가져오기
    // InstanceNumber 기준으로 정렬해서 반환
    @SuppressWarnings("unchecked")
    public List<String> getInstanceIdsBySeries(Long seriesKey) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND)); // STUDY_NOT_FOUND -> SERIES_NOT_FOUND

        if (series.getOrthancSeriesId() == null) {
            log.warn("orthancSeriesId가 없습니다. seriesKey: {} (동기화 필요)", seriesKey);
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        // expand=true로 instance별 MainDicomTags(InstanceNumber 포함)까지 한 번에 조회
        String url = orthancUrl + "/series/" + series.getOrthancSeriesId() + "/instances?expand=true";
        List<Map<String, Object>> instances = restTemplate.getForObject(url, List.class);

        if (instances == null || instances.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return instances.stream()
                .sorted(Comparator.comparingInt(this::extractInstanceNumber))
                .map(inst -> (String) inst.get("ID"))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private int extractInstanceNumber(Map<String, Object> instance) {
        try {
            Map<String, Object> tags = (Map<String, Object>) instance.get("MainDicomTags");
            String num = (String) tags.get("InstanceNumber");
            return num != null ? Integer.parseInt(num.trim()) : Integer.MAX_VALUE;
        } catch (Exception e) {
            return Integer.MAX_VALUE; // 파싱 실패 시 맨 뒤로 밀어서 순서를 깨뜨리지 않음
        }
    }

    public DicomResponseDto.StudyDto getStudyDetail(Long studyKey) {
        Study study = studyRepository.findById(studyKey)
                .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));

        Patient patient = study.getPatient();

        return new DicomResponseDto.StudyDto(
                study.getStudyKey(),
                study.getDescription(),
                study.getStudyDateTime(),
                study.getTotalSeriesCount(),
                study.getTotalInstanceCount(),
                study.getAllowedResearch() != null && study.getAllowedResearch() == 1,
                study.getDelFlag() != null && study.getDelFlag() == 1,
                patient.getPName(),
                patient.getPBirth()
        );
    }

    // Study에 속한 Series 목록
    public List<DicomResponseDto.SeriesDto> getSeriesByStudy(Long studyKey) {
        if (!studyRepository.existsById(studyKey)) {
            throw new BaseException(ErrorCode.STUDY_NOT_FOUND);
        }

        List<Series> seriesEntities = seriesRepository.findByStudy_StudyKeyAndDelFlag(studyKey, 0);

        return seriesEntities.stream()
                // PR/SR/KO 등 픽셀 데이터 없는 시리즈는 이미지 뷰어 목록에서 제외 (DB에는 그대로 남아있음)
                .filter(s -> s.getModality() == null || !NON_IMAGE_MODALITIES.contains(s.getModality().toUpperCase()))
                .map(s -> new DicomResponseDto.SeriesDto(
                        s.getSeriesKey(),
                        s.getSeriesNum(),
                        null,
                        s.getSeriesNum(),
                        s.getBodyPart(),
                        s.getTotalInstanceCount(),
                        s.getSeriesDescription(),
                        s.getDelFlag() != null && s.getDelFlag() == 1
                ))
                .collect(Collectors.toList());
    }

    // 인스턴스 하나의 raw DICOM 바이너리 프록시
    // seriesKey를 같이 받아서, 해당 시리즈에 실제로 속한 인스턴스인지 검증 (임의 UUID 요청 방지)
    public StreamingResponseBody getInstanceFile(Long seriesKey, String instanceId) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));

        if (series.getOrthancSeriesId() == null) {
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        List<String> validInstanceIds = getInstanceIdsBySeries(seriesKey);
        if (!validInstanceIds.contains(instanceId)) {
            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND);
        }

        String url = orthancUrl + "/instances/" + instanceId + "/file";

        return outputStream -> restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
            StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
            return null;
        });
    }

    // AI 추론용: 인스턴스 하나의 raw DICOM byte[] 조회
    public byte[] getInstanceBytes(Long seriesKey, String instanceId) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));

        if (series.getOrthancSeriesId() == null) {
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        List<String> validInstanceIds = getInstanceIdsBySeries(seriesKey);
        if (!validInstanceIds.contains(instanceId)) {
            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND);
        }

        String url = orthancUrl + "/instances/" + instanceId + "/file";
        return restTemplate.getForObject(url, byte[].class);
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