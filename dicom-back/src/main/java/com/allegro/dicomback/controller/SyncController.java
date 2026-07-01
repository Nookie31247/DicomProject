package com.allegro.dicomback.controller;

import com.allegro.dicomback.service.DicomSyncService;
import com.allegro.dicomback.service.PatientSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class SyncController {

    private final PatientSyncService patientSyncService;
    private final DicomSyncService dicomSyncService;

//    @PostMapping("/patients")
//    public ResponseEntity<String> syncPatients() {
//        patientSyncService.syncPatientsFromOrthanc();
//        return ResponseEntity.ok("환자 정보 동기화가 성공적으로 완료되었습니다.");
//    }
//
//    @PostMapping("/studies")
//    public ResponseEntity<String> syncStudies() {
//        dicomSyncService.syncAllStudies();
//        return ResponseEntity.ok("검사 목록 데이터 동기화 성공.");
//    }

    @PostMapping("/all")
    public ResponseEntity<String> syncAll() {
        patientSyncService.syncPatientsFromOrthanc();
        dicomSyncService.syncAllStudies();
        return ResponseEntity.ok("동기화 완료");
    }
}