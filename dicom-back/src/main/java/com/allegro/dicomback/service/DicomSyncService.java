//package com.allegro.dicomback.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class DicomSyncService {
//
//    private final RestTemplate restTemplate;
//    private final DicomSyncHelperService dicomSyncHelperService;
//
//    private final String ORTHANC_URL = "http://localhost:8042";
//
//    // 처리 데이터가 많아 오래 걸릴 수 있어서 Async
//    @Async
//    public void syncAllStudies() {
//        log.info("### 전체 동기화 작업을 시작합니다.");
//
//        // 1. ParameterizedTypeReference를 사용하여 타입 안정성 확보
//        ResponseEntity<List<String>> response = restTemplate.exchange(
//                ORTHANC_URL + "/studies",
//                HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<>() {}
//        );
//
//        List<String> allStudyUuids = response.getBody();
//
//        // 2. null 체크 추가 (가장 중요!)
//        if (allStudyUuids == null || allStudyUuids.isEmpty()) {
//            log.warn("Orthanc에서 가져온 Study 목록이 비어있습니다.");
//            CompletableFuture.completedFuture(null);
//            return;
//        }
//
//        // [디버깅] 가져온 데이터 확인
//        if (allStudyUuids != null) {
//            log.info("### Orthanc에서 가져온 Study 개수: {}", allStudyUuids.size());
//        } else {
//            log.warn("### allStudyUuids가 null입니다.");
//            return;
//        }
//
//        // 3. 루프 처리
//        for (String uuid : allStudyUuids) {
//            try {
////                log.info("### 동기화 중인 UUID: {}", uuid); // 루프가 도는지 확인
//                dicomSyncHelperService.syncStudy(uuid);
//            } catch (Exception e) {
//                log.error("전체 동기화 중 오류 발생 - Study ID: {}", uuid, e);
//            }
//        }
//
//        log.info("### 전체 동기화 작업이 완료되었습니다.");
//        CompletableFuture.completedFuture(null);
//    }
//}