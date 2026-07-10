package com.allegro.dicomback.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DICOM 검사(study)를 익명화하는 서비스입니다.
 */
@Slf4j
@Service
public class AnonymizationService {

    private final RestClient orthancRestClient;
    private final RestClient anonSpringRestClient;

    /**
     * AnonymizationService를 생성합니다.
     *
     * @param restClientBuilder REST 클라이언트 빌더
     * @param orthancUrl Orthanc 서버 URL
     * @param anonymizationSpringUrl 익명화 스프링 서버 URL
     */
    public AnonymizationService(
            RestClient.Builder restClientBuilder,
            @Value("${orthanc.url}") String orthancUrl,
            @Value("${server.anonAppUrl}") String anonymizationSpringUrl
    ) {
        this.orthancRestClient = restClientBuilder
                .baseUrl(orthancUrl)
                .build();

        this.anonSpringRestClient = RestClient.builder()
                .baseUrl(anonymizationSpringUrl)
                .build();
    }

    private record OrthancRes(
            @JsonProperty("ID") String id,
            @JsonProperty("Path") String path,
            @JsonProperty("Type") String type
    ) {}

    private record AnonymizedStudyUid(
            @JsonProperty("study-uid") String studyUid
    ) {}

    private record OrthancStudyDetail(
            @JsonProperty("ID") String id,
            @JsonProperty("MainDicomTags") Map<String, String> mainDicomTags
    ) {}

    /**
     * Orthanc UID를 통해 검사(study) 목록을 익명화합니다.
     *
     * @param orthancUidToDateOffsetDays
     * 익명화할 Orthanc Study ID -> 그 Study 환자의 날짜 오프셋(일수).
     * 오프셋 계산은 DicomService.computeDateOffsetDays().
     */
    public void anonymize(Map<String, Integer> orthancUidToDateOffsetDays) {
        if (orthancUidToDateOffsetDays == null || orthancUidToDateOffsetDays.isEmpty()) {
            return;
        }

        // 1단계: 원본 Orthanc에서 Study 익명화
        List<String> anonymizationOrthancIds = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : orthancUidToDateOffsetDays.entrySet()) {
            String uid = entry.getKey();
            int offsetDays = entry.getValue();

            // Orthanc에 익명화를 요청하기 전에, 정정본 Study의 진짜 StudyDate를 먼저 읽어서
            // 환자별 오프셋만큼 밀어준 값을 계산해둔다. 이후 anonymize 호출에서 이 값을 Replace로 넘긴다.
            // (실패하면 null이 오고, 그 경우 아래에서 StudyDate는 그냥 Orthanc 기본 동작에 맡긴다)
            String shiftedStudyDate = computeShiftedStudyDate(uid, offsetDays);

            // anonymize 요청 바디를 더 이상 빈 값(Map.of())으로 보내지 않는다.
            // - Keep: StudyDescription은 자유 텍스트지만 연구 가치가 있는 정보라 기본 삭제 대신 유지.
            // - Replace: StudyDate를 환자별 오프셋만큼 밀어준 값으로 지정(계산에 성공했을 때만).
            Map<String, Object> anonymizeBody = new HashMap<>();
            anonymizeBody.put("Keep", List.of("StudyDescription"));
            if (shiftedStudyDate != null) {
                anonymizeBody.put("Replace", Map.of("StudyDate", shiftedStudyDate));
            }

            OrthancRes res = orthancRestClient.post()
                    .uri("/studies/{id}/anonymize", uid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(anonymizeBody)
                    .retrieve()
                    .body(OrthancRes.class);

            if (res == null || res.id() == null) {
                throw new IllegalStateException("익명화 실패. studyId=" + uid);
            }

            anonymizationOrthancIds.add(res.id());
        }

        // 2단계: 익명화된 Study의 StudyInstanceUID 조회
        List<AnonymizedStudyUid> anonymizedStudyUids = new ArrayList<>();

        for (String anonymizedOrthancId : anonymizationOrthancIds) {
            OrthancStudyDetail studyDetail = orthancRestClient.get()
                    .uri("/studies/{id}", anonymizedOrthancId)
                    .retrieve()
                    .body(OrthancStudyDetail.class);

            if (studyDetail == null || studyDetail.mainDicomTags() == null) {
                throw new IllegalStateException("익명화된 Study 조회 실패. orthancId=" + anonymizedOrthancId);
            }

            String studyInstanceUid = studyDetail.mainDicomTags().get("StudyInstanceUID");

            if (studyInstanceUid == null || studyInstanceUid.isBlank()) {
                throw new IllegalStateException("StudyInstanceUID 없음. orthancId=" + anonymizedOrthancId);
            }

            anonymizedStudyUids.add(new AnonymizedStudyUid(studyInstanceUid));
        }

        // 3단계: 익명화된 Study를 익명화 Orthanc 서버로 전송
        orthancRestClient.post()
                .uri("/modalities/{modality}/store", "anonymization-server")
                .contentType(MediaType.APPLICATION_JSON)
                .body(anonymizationOrthancIds)
                .retrieve()
                .toBodilessEntity();

        // 4단계: 익명화 Spring 서버에 StudyInstanceUID 전달
        anonSpringRestClient.post()
                .uri("/api/research/dicom/get-anonymization")
                .contentType(MediaType.APPLICATION_JSON)
                .body(anonymizedStudyUids)
                .retrieve()
                .toBodilessEntity();

        // 5단계: 전송과 알림이 모두 성공한 뒤 원본 Orthanc의 임시 익명화 Study 삭제
        for (String anonymizedOrthancId : anonymizationOrthancIds) {
            orthancRestClient.delete()
                    .uri("/studies/{id}", anonymizedOrthancId)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    /**
     * 수정본 Study(아직 익명화 전, orthancUid)의 실제 StudyDate를 Orthanc에서 읽어와서,
     * 환자별 오프셋(offsetDays)만큼 밀어준 DICOM 날짜 문자열(yyyyMMdd)을 계산합니다.
     *
     * 실패(태그가 없거나 조회 실패 등)하면 null을 반환한다 — 이 경우 호출하는 쪽(anonymize())은
     * StudyDate를 Replace에 넣지 않고, Orthanc의 기본 익명화 동작에 그대로 맡긴다
     *
     * @param studyOrthancId 수정본 Study의 Orthanc ID (익명화 대상, 아직 익명화 전 상태)
     * @param offsetDays 이 Study 환자의 날짜 시프트 오프셋(일수)
     * @return 밀린 날짜 문자열(yyyyMMdd), 계산할 수 없으면 null
     */
    private String computeShiftedStudyDate(String studyOrthancId, int offsetDays) {
        try {
            OrthancStudyDetail detail = orthancRestClient.get()
                    .uri("/studies/{id}", studyOrthancId)
                    .retrieve()
                    .body(OrthancStudyDetail.class);

            if (detail == null || detail.mainDicomTags() == null) {
                return null;
            }

            String originalStudyDate = detail.mainDicomTags().get("StudyDate");
            if (originalStudyDate == null || originalStudyDate.isBlank()) {
                return null;
            }

            LocalDate parsed = LocalDate.parse(originalStudyDate.trim(), DateTimeFormatter.BASIC_ISO_DATE);
            return parsed.plusDays(offsetDays).format(DateTimeFormatter.BASIC_ISO_DATE);
        } catch (RuntimeException e) {
            // 날짜 파싱 실패든, Orthanc 조회 실패든 — 이 부가 기능 하나 때문에 전체 익명화 흐름을
            // 막을 필요는 없으므로 경고만 남기고 null로 폴백한다.
            log.warn("StudyDate 시프트 계산 실패(기본 익명화 동작으로 폴백). studyOrthancId={}, 원인={}",
                    studyOrthancId, e.getMessage());
            return null;
        }
    }
}
