package com.allegro.dicomback.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnonymizationService {

    private final RestClient orthancRestClient;
    private final RestClient anonSpringRestClient;

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

    public void anonymize(List<String> orthancUids) {
        if (orthancUids == null || orthancUids.isEmpty()) {
            return;
        }

        // 1단계: 원본 Orthanc에서 Study 익명화
        List<String> anonymizationOrthancIds = new ArrayList<>();

        for (String uid : orthancUids) {
            OrthancRes res = orthancRestClient.post()
                    .uri("/studies/{id}/anonymize", uid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
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
}
