package com.allegro.dicomback.service;

import com.allegro.dicomback.dto.DicomResponseDto;
import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.Series;
import com.allegro.dicomback.entity.Study;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.SeriesRepository;
import com.allegro.dicomback.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 관련 DICOM 작업을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {
    private final SeriesRepository seriesRepository;
    private final StudyRepository studyRepository;
    private final RestTemplate restTemplate;
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

    /**
     * 환자 이름 및 생년월일을 포함하여 뷰어 페이지에 대한 검사(study) 세부 정보를 검색합니다.
     *
     * @param studyKey 검사(study) 키
     * @return 검사(study) 세부 정보 DTO
     */
    public DicomResponseDto.StudyDetailDto getStudyDetail(Long studyKey) {
        Study study = studyRepository.findById(studyKey)
                .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));

        Patient patient = study.getPatientKey();

        List<SeriesRepository.SeriesAndImagesCount> counts =seriesRepository.getSeriesAndImagesCount(List.of(studyKey));

        Long seriesNum = counts.isEmpty() ? 0L : counts.get(0).getSeriesNum();
        Long imagesNum = counts.isEmpty() ? 0L : counts.get(0).getImagesNum();

        DicomResponseDto.PatientSummaryDto patientSummary = new DicomResponseDto.PatientSummaryDto(
                patient.getName(),
                patient.getBirth()
        );

        return new DicomResponseDto.StudyDetailDto(
                study.getKey(),
                study.getDescription(),
                study.getCreatedAt(),
                seriesNum,
                imagesNum,
                study.getAllowResearch() != null && study.getAllowResearch(),
                study.getHiddenFlag() != null && study.getHiddenFlag(),
                patientSummary
        );
    }

    /**
     * 뷰어 페이지에 표시될 주어진 검사(study)에 대한 시리즈 목록을 검색합니다.
     *
     * @param studyKey 검사(study) 키
     * @return 시리즈 DTO 목록
     */
    public List<DicomResponseDto.SeriesDto> getSeriesByStudy(Long studyKey) {
        if (!studyRepository.existsById(studyKey)) {
            throw new BaseException(ErrorCode.STUDY_NOT_FOUND);
        }

        List<Series> seriesEntities = seriesRepository.findByStudyKey_KeyAndHiddenFlag(studyKey, false);

        return seriesEntities.stream()
                // PR/SR/KO 등 픽셀 데이터 없는 시리즈는 이미지 뷰어 목록에서 제외 (DB에는 그대로 남아있음)
                .filter(s -> s.getModality() == null || !NON_IMAGE_MODALITIES.contains(s.getModality().toUpperCase()))
                .map(s -> new DicomResponseDto.SeriesDto(
                        s.getKey(),
                        s.getSeriesNum(),
                        s.getCreatedAt(),
                        s.getSeriesNum(),
                        s.getBodyPart(),
                        s.getSeriesDescription(),
                        s.getHiddenFlag() != null && s.getHiddenFlag()
                ))
                .collect(Collectors.toList());
    }

    /**
     * InstanceNumber를 기준으로 특정 시리즈에 대한 인스턴스 ID를 검색하고 정렬합니다.
     *
     * @param seriesKey 시리즈 키
     * @return 정렬된 인스턴스 ID 목록
     */
    @SuppressWarnings("unchecked")
    public List<String> getInstanceIdsBySeries(Long seriesKey) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));

        if (series.getOrthancId() == null) {
            log.warn("orthancSeriesId가 없습니다. seriesKey: {} (동기화 필요)", seriesKey);
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        // expand=true로 instance별 MainDicomTags(InstanceNumber 포함)까지 한 번에 조회
        String url = orthancUrl + "/series/" + series.getOrthancId() + "/instances?expand=true";
        List<Map<String, Object>> instances = restTemplate.getForObject(url, List.class);

        if (instances == null || instances.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return instances.stream()
                .sorted(Comparator.comparingInt(this::extractInstanceNumber))
                .map(inst -> (String) inst.get("ID"))
                .collect(Collectors.toList());
    }

    /**
     * 프론트엔드 뷰어 목록을 위한 인스턴스 정보를 검색합니다.
     *
     * @param seriesKey 시리즈 키
     * @return 인스턴스 정보 DTO 목록
     */
    @SuppressWarnings("unchecked")
    public List<DicomResponseDto.InstanceInfoDto> getInstancesBySeries(Long seriesKey) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));

        if (series.getOrthancId() == null) {
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        String url = orthancUrl + "/series/" + series.getOrthancId() + "/instances?expand=true";
        List<Map<String, Object>> instances = restTemplate.getForObject(url, List.class);

        if (instances == null || instances.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return instances.stream()
                .sorted(Comparator.comparingInt(this::extractInstanceNumber))
                .map(inst -> new DicomResponseDto.InstanceInfoDto(
                        (String) inst.get("ID"),
                        extractNumberOfFrames(inst)
                ))
                .collect(Collectors.toList());
    }


    /**
     * MainDicomTags에서 프레임 수를 추출합니다.
     * 태그가 아예 없으면(일반 단일 프레임 이미지) 1장으로 취급.
     *
     * @param instance 인스턴스 맵
     * @return 프레임 수
     */
    @SuppressWarnings("unchecked")
    private int extractNumberOfFrames(Map<String, Object> instance) {
        try {
            Map<String, Object> tags = (Map<String, Object>) instance.get("MainDicomTags");
            String num = (String) tags.get("NumberOfFrames");
            return (num != null && !num.isBlank()) ? Integer.parseInt(num.trim()) : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 단일 인스턴스에 대한 원본 DICOM 파일을 스트리밍 응답으로 검색합니다.
     * Viewer 페이지에서 Dicom 이미지를 띄우는 기능.
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

    /**
     * AI 추론을 위해 원본 DICOM 바이트 배열을 검색합니다.
     *
     * @param seriesKey 시리즈 키
     * @param instanceId 인스턴스 ID
     * @return 인스턴스의 바이트 배열
     */
    public byte[] getInstanceBytes(Long seriesKey, String instanceId) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));

        if (series.getOrthancId() == null) {
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        List<String> validInstanceIds = getInstanceIdsBySeries(seriesKey);
        if (!validInstanceIds.contains(instanceId)) {
            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND);
        }

        String url = orthancUrl + "/instances/" + instanceId + "/file";
        return restTemplate.getForObject(url, byte[].class);
    }

    private int extractInstanceNumber(Map<String, Object> instance) {
        try {
            Map<String, Object> tags = (Map<String, Object>) instance.get("MainDicomTags");
            String num = (String) tags.get("InstanceNumber");
            return num != null ? Integer.parseInt(num.trim()) : Integer.MAX_VALUE;
        } catch (Exception e) {
            return Integer.MAX_VALUE; // 파싱 실패 시 맨 뒤로 밀어서 순서를 깨뜨리지 않음
        }
    }
}
