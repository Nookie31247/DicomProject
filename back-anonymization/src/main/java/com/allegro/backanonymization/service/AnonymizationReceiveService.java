package com.allegro.backanonymization.service;

import com.allegro.backanonymization.dto.AnonymizationRequestDto;
import com.allegro.backanonymization.entity.Series;
import com.allegro.backanonymization.entity.Study;
import com.allegro.backanonymization.repository.SeriesRepository;
import com.allegro.backanonymization.repository.StudyRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
public class AnonymizationReceiveService {

    private final RestClient orthancRestClient;
    private final StudyRepository studyRepository;
    private final SeriesRepository seriesRepository;

    private static final DateTimeFormatter DICOM_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    public AnonymizationReceiveService(
            RestClient.Builder restClientBuilder,
            @Value("${orthanc.url}") String orthancUrl,
            StudyRepository studyRepository,
            SeriesRepository seriesRepository
    ) {
        this.orthancRestClient = restClientBuilder
                .baseUrl(orthancUrl)
                .build();
        this.studyRepository = studyRepository;
        this.seriesRepository = seriesRepository;
    }

    // ---- Orthanc 응답 DTO ----

    private record OrthancStudyDetail(
            @JsonProperty("ID") String id,
            @JsonProperty("MainDicomTags") Map<String, String> mainDicomTags,
            @JsonProperty("PatientMainDicomTags") Map<String, String> patientMainDicomTags,
            @JsonProperty("Series") List<String> seriesOrthancIds
    ) {}

    private record OrthancSeriesDetail(
            @JsonProperty("ID") String id,
            @JsonProperty("MainDicomTags") Map<String, String> mainDicomTags,
            @JsonProperty("Instances") List<String> instanceOrthancIds
    ) {}

    // ---- 메인 로직 ----

    @Transactional
    public void saveStudies(List<AnonymizationRequestDto> request) {
        if (request == null || request.isEmpty()) {
            return;
        }

        for (AnonymizationRequestDto data : request) {
            saveOneStudy(data.studyUid());
        }
    }

    private void saveOneStudy(String studyInstanceUid) {
        // 이미 저장된 Study면 중복 저장 방지
        if (studyRepository.existsByUid(studyInstanceUid)) {
            return;
        }

        // 1단계: StudyInstanceUID → 익명화 Orthanc 내부 ID 조회
        List<String> orthancIds = findOrthancIdByStudyUid(studyInstanceUid);
        if (orthancIds.isEmpty()) {
            throw new IllegalStateException("Study를 찾을 수 없음. studyUid=" + studyInstanceUid);
        }
        String studyOrthancId = orthancIds.getFirst();

        // 2단계: Study 상세 조회
        OrthancStudyDetail studyDetail = orthancRestClient.get()
                .uri("/studies/{id}", studyOrthancId)
                .retrieve()
                .body(OrthancStudyDetail.class);

        if (studyDetail == null || studyDetail.mainDicomTags() == null) {
            throw new IllegalStateException("Study 상세 조회 실패. orthancId=" + studyOrthancId);
        }

        Map<String, String> studyTags = studyDetail.mainDicomTags();
        Map<String, String> patientTags = studyDetail.patientMainDicomTags() != null
                ? studyDetail.patientMainDicomTags()
                : Map.of();

        String actualStudyUid = studyTags.getOrDefault("StudyInstanceUID", studyInstanceUid);

        Study study = Study.builder()
                .uid(actualStudyUid)
                .orthancId(studyOrthancId)
                .patientBirth(parseDicomDate(patientTags.get("PatientBirthDate")))
                .patientSex(patientTags.get("PatientSex"))
                .description(studyTags.get("StudyDescription"))
                .build();

        Study savedStudy = studyRepository.save(study);

        // 3단계: 하위 Series들 조회 및 저장
        List<String> seriesOrthancIds = studyDetail.seriesOrthancIds() != null
                ? studyDetail.seriesOrthancIds()
                : List.of();

        for (String seriesOrthancId : seriesOrthancIds) {
            saveOneSeries(seriesOrthancId, savedStudy);
        }
    }

    private void saveOneSeries(String seriesOrthancId, Study study) {
        OrthancSeriesDetail seriesDetail = orthancRestClient.get()
                .uri("/series/{id}", seriesOrthancId)
                .retrieve()
                .body(OrthancSeriesDetail.class);

        if (seriesDetail == null || seriesDetail.mainDicomTags() == null) {
            throw new IllegalStateException("Series 상세 조회 실패. orthancId=" + seriesOrthancId);
        }

        Map<String, String> seriesTags = seriesDetail.mainDicomTags();
        String seriesInstanceUid = seriesTags.get("SeriesInstanceUID");

        if (seriesInstanceUid != null && seriesRepository.existsByUid(seriesInstanceUid)) {
            return; // 이미 저장된 Series면 스킵
        }

        int imageCount = seriesDetail.instanceOrthancIds() != null
                ? seriesDetail.instanceOrthancIds().size()
                : 0;

        Series series = Series.builder()
                .uid(seriesInstanceUid)
                .studyKey(study)
                .seriesNum(parseInteger(seriesTags.get("SeriesNumber")))
                .bodyPart(seriesTags.get("BodyPartExamined"))
                .modality(seriesTags.get("Modality"))
                .orthancId(seriesOrthancId)
                .totalImagesCount(imageCount)
                .build();

        seriesRepository.save(series);
    }

    private List<String> findOrthancIdByStudyUid(String studyInstanceUid) {
        Map<String, Object> body = Map.of(
                "Level", "Study",
                "Query", Map.of("StudyInstanceUID", studyInstanceUid)
        );

        List<String> result = orthancRestClient.post()
                .uri("/tools/find")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {});

        return result != null ? result : List.of();
    }

    private LocalDate parseDicomDate(String dicomDate) {
        if (dicomDate == null || dicomDate.isBlank() || "0".equals(dicomDate)) {
            return null;
        }
        try {
            return LocalDate.parse(dicomDate, DICOM_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}