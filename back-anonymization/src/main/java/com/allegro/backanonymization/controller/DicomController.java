package com.allegro.backanonymization.controller;

import com.allegro.backanonymization.config.JwtTokenProvider;
import com.allegro.backanonymization.dto.DicomRequestDto.*;
import com.allegro.backanonymization.dto.DicomResponseDto.*;
import com.allegro.backanonymization.service.DicomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/api/dicom")
@RequiredArgsConstructor //의존성 주입
public class DicomController {
    private final DicomService dicomService;
    private final JwtTokenProvider jwtTokenProvider;

    //환자 목록 불러오기
    @GetMapping("/patients")
    public ResponseEntity<List<PatientDto>> getPatients(
            @CookieValue(name = "token") String token,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String search
    ) {
        Long doctorKey = jwtTokenProvider.getUserKey(token);
        return ResponseEntity.ok(dicomService.getPatients(doctorKey, start, end, search));
    }

    //스터디 목록 불러오기
    @GetMapping("/studies")
    public ResponseEntity<List<StudyDto>> getStudies(
            @CookieValue(name = "token") String token,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(name = "patient-key") Long patientKey,
            @RequestParam(required = false) String search
    ) {
        Long doctorKey = jwtTokenProvider.getUserKey(token);
        return ResponseEntity.ok(dicomService.getStudies(doctorKey, patientKey, start, end, search));
    }

    //시리즈 목록 불러오기
    @GetMapping("/series")
    public ResponseEntity<List<SeriesDto>> getSeries(
            @CookieValue(name = "token") String token,
            @RequestParam(name = "study-key") Long studyKey
        ) {
        Long doctorKey = jwtTokenProvider.getUserKey(token);
        return ResponseEntity.ok(dicomService.getSeries(doctorKey, studyKey));
    }

    //데이터 익명화 허용하기 (Study 단위)
    @GetMapping("/studies/anonymization")
    public ResponseEntity<Void> anonymizeStudies(
            @RequestParam("study-key")Long studyKey
    ) {
        return ResponseEntity.ok().build();
    }

    //환자 목록 숨기기/보이기 설정
    @PostMapping("/patients/hide")
    public ResponseEntity<Void> hidePatients(
            @CookieValue(name = "token") String token,
            @RequestBody List<PatientHideDto> request
    ) {
        Long doctorKey = jwtTokenProvider.getUserKey(token);
        dicomService.setHidePatients(doctorKey, request);
        return ResponseEntity.noContent().build();
    }

    // 스터디 목록 숨기기/보이기 설정
    @PostMapping("/studies/hide")
    public ResponseEntity<Void> hideStudies(
            @CookieValue(name = "token") String token,
            @RequestBody List<StudyHideDto> request
    ) {
        Long doctorKey = jwtTokenProvider.getUserKey(token);
        dicomService.setHideStudies(doctorKey, request);
        return ResponseEntity.noContent().build();
    }

    //시리즈 목록 숨기기/보이기 설정
    @PostMapping("/series/hide")
    public ResponseEntity<Void> hideSeries(
            @CookieValue(name = "token") String token,
            @RequestBody List<SeriesHideDto> request
    ) {
        Long doctorKey = jwtTokenProvider.getUserKey(token);
        dicomService.setHideSeries(doctorKey, request);
        return ResponseEntity.noContent().build();
    }

    //스터디 다운로드
    @GetMapping("/studies/download")
    public ResponseEntity<StreamingResponseBody> downloadStudies(
            @RequestParam("study-key") Long studyKey
    ) {
        StreamingResponseBody stream = dicomService.downloadStudyAsZip(studyKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"study_" + studyKey + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    //시리즈 다운로드
    @GetMapping("/series/download")
    public ResponseEntity<StreamingResponseBody> downloadSeries(
            @RequestParam("series-key") Long seriesKey
    ) {
        StreamingResponseBody stream = dicomService.downloadSeriesAsZip(seriesKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"series_" + seriesKey + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

}
