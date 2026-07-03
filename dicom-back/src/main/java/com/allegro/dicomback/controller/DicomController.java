package com.allegro.dicomback.controller;

import com.allegro.dicomback.dto.DicomRequestDto.*;
import com.allegro.dicomback.dto.DicomResponseDto.*;
import com.allegro.dicomback.service.DicomService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import com.allegro.dicomback.service.DicomService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.netty.http.server.HttpServerResponse;

import java.util.List;

@RestController
@RequestMapping("/api/dicom")
@RequiredArgsConstructor //의존성 주입
public class DicomController {
    private final DicomService dicomService;

    //환자 목록 불러오기
    @GetMapping("/patients")
    public ResponseEntity<List<PatientDto>> getPatients(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(dicomService.getPatients(start, end, search));
    }

    //스터디 목록 불러오기
    @GetMapping("/studies")
    public ResponseEntity<List<StudyDto>> getStudies() {
        return ResponseEntity.ok().build();
    }

    //시리즈 목록 불러오기
    @GetMapping("/series")
    public ResponseEntity<List<SeriesDto>> getSeries() {
        return ResponseEntity.ok().build();
    }

    //이미지 목록 불러오기
    @GetMapping("/images")
    public ResponseEntity<List<ImageDto>> getImages() {
        return ResponseEntity.ok().build();
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
    public ResponseEntity<Void> hidePatients(@RequestBody
            List<PatientHideDto> request
    ) {
        return ResponseEntity.noContent().build();
    }

    // 스터디 목록 숨기기/보이기 설정
    @PostMapping("/studies/hide")
    public ResponseEntity<Void> hideStudies(
            @RequestBody List<StudyHideDto> request) {
        return ResponseEntity.noContent().build();
    }

    //시리즈 목록 숨기기/보이기 설정
    @PostMapping("/series/hide")
    public ResponseEntity<Void> hideSeries(
            @RequestBody List<SeriesHideDto> request) {
        return ResponseEntity.noContent().build();
    }


    //이미지 목록 숨기기/보이기 설정
    @PostMapping("/images/hide")
    public ResponseEntity<Void> hideImages(
            @RequestBody List<ImageHideDto> request
    ) {
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

//    @GetMapping("/studies/download")
//    public void downloadStudies(
//            @RequestParam("study-key") Long studyKey,
//            HttpServletResponse response
//    ) {
//        dicomService.downloadStudyAsZip(studyKey, response);
//    }

//    시리즈 다운로드
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

//    @GetMapping("/series/download")
//    public void downloadSeries(
//            @RequestParam("series-key") Long seriesKey,
//            HttpServletResponse response
//    ) {
//        dicomService.downloadSeriesAsZip(seriesKey, response);
//    }


//    //이미지 다운로드
//    @GetMapping("/images/download")
//    public ResponseEntity<Resource> downloadImages(
//            @RequestParam ("image-key") Long imageKey
//    ) {
//        Resource resource = dicomService.downloadImage(imageKey);
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"image_" + imageKey + ".dcm\"")
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(resource);
//    }
//    //Orthanc의 내용가져오기(이제 안 쓸듯?)
//    private final OrthancSyncService orthancSyncService;
//
//    @PostMapping("/sync")
//    public ResponseEntity<String> sync() {
//        orthancSyncService.syncInstancesFromOrthanc();
//        return ResponseEntity.ok("동기화 성공");
//    }

    // DicomController.java
    @GetMapping("/series/{seriesKey}/instances")
    public ResponseEntity<List<String>> getInstancesOfSeries(@PathVariable("seriesKey") Long seriesKey) {
        return ResponseEntity.ok(dicomService.getInstanceIdsBySeries(seriesKey));
    }

    //Study 상세
    @GetMapping("/studies/{studyKey}")
    public ResponseEntity<DicomResponseDto.StudyDto> getStudyDetail(@PathVariable Long studyKey) {
        return ResponseEntity.ok(dicomService.getStudyDetail(studyKey));
    }

    //Study에 속한 Series 목록
    @GetMapping("/studies/{studyKey}/series")
    public ResponseEntity<List<DicomResponseDto.SeriesDto>> getSeriesOfStudy(@PathVariable Long studyKey) {
        return ResponseEntity.ok(dicomService.getSeriesByStudy(studyKey));
    }

    //인스턴스 raw DICOM 스트리밍
    @GetMapping("/series/{seriesKey}/instances/{instanceId}/file")
    public ResponseEntity<StreamingResponseBody> getInstanceFile(
            @PathVariable Long seriesKey,
            @PathVariable String instanceId
    ) {
        StreamingResponseBody stream = dicomService.getInstanceFile(seriesKey, instanceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/dicom"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + instanceId + ".dcm\"")
                .body(stream);
    }
}
