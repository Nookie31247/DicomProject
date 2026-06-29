package com.allegro.dicomback.controller;

import com.allegro.dicomback.dto.DicomRequestDto;
import com.allegro.dicomback.dto.DicomResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dicom")
@RequiredArgsConstructor //의존성 주입
public class DicomController {

    //환자 목록 불러오기
    @GetMapping("/patients")
    public ResponseEntity<List<DicomResponseDto.PatientDto>> getPatients(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok().build();
    }

    //스터디 목록 불러오기
    @GetMapping("/studies")
    public ResponseEntity<List<DicomResponseDto.StudyDto>> getStudies() {
        return ResponseEntity.ok().build();
    }

    //시리즈 목록 불러오기
    @GetMapping("/series")
    public ResponseEntity<List<DicomResponseDto.SeriesDto>> getSeries() {
        return ResponseEntity.ok().build();
    }

    //이미지 목록 불러오기
    @GetMapping("/images")
    public ResponseEntity<List<DicomResponseDto.ImageDto>> getImages() {
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
            List<DicomRequestDto.PatientHideDto> request
    ) {
        return ResponseEntity.noContent().build();
    }

    // 스터디 목록 숨기기/보이기 설정
    @PostMapping("/studies/hide")
    public ResponseEntity<Void> hideStudies(
            @RequestBody List<DicomRequestDto.StudyHideDto> request) {
        return ResponseEntity.noContent().build();
    }

    //시리즈 목록 숨기기/보이기 설정
    @PostMapping("/series/hide")
    public ResponseEntity<Void> hideSeries(
            @RequestBody List<DicomRequestDto.SeriesHideDto> request) {
        return ResponseEntity.noContent().build();
    }


    //이미지 목록 숨기기/보이기 설정
    @PostMapping("/images/hide")
    public ResponseEntity<Void> hideImages(
            @RequestBody List<DicomRequestDto.ImageHideDto> request
    ) {
        return ResponseEntity.noContent().build();
    }

    //스터디 다운로드
    @GetMapping("/studies/download")
    public ResponseEntity<Void> downloadStudies(
            @RequestParam("study-key") Long studyKey
    ) {
        return ResponseEntity.ok().build();
    }

    //시리즈 다운로드
    @GetMapping("/series/download")
    public ResponseEntity<Void> downloadSeries(
            @RequestParam ("series-key")  Long seriesKey
    ) {
        return ResponseEntity.ok().build();
    }

    //이미지 다운로드
    @GetMapping("/images/download")
    public ResponseEntity<Void> downloadImages(
            @RequestParam ("image-key") Long imageKey
    ) {
        return ResponseEntity.ok().build();
    }
}
