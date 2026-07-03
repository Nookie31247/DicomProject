package com.allegro.dicomback.controller;

import com.allegro.dicomback.service.DicomSyncService;
import com.allegro.dicomback.service.PatientSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class SyncController {

    private final PatientSyncService patientSyncService;
    private final DicomSyncService dicomSyncService;

    // Total Sync
    @PostMapping
    public ResponseEntity<String> allSyncData() {
        patientSyncService.syncPatientsFromOrthanc();
        log.info("환자 정보 동기화가 성공적으로 완료되었습니다.");
        dicomSyncService.syncAllStudies();
        log.info("검사 목록 데이터 동기화 성공.");
        return ResponseEntity.ok("모든 정보 동기화가 완료되었습니다.");
    }

    @PostMapping("/patients")
    public ResponseEntity<String> syncPatients() {
        patientSyncService.syncPatientsFromOrthanc();

        return ResponseEntity.ok("환자 정보 동기화가 성공적으로 완료되었습니다.");
    }

    // 사전에 patients가 없으면 오류남(patients->studies) 순서대로
    @PostMapping("/studies")
    public ResponseEntity<String> syncStudies() {
        dicomSyncService.syncAllStudies();
        return ResponseEntity.ok("검사 목록 데이터 동기화 성공.");
    }
}