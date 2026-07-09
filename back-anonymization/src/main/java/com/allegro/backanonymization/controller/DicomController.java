package com.allegro.backanonymization.controller;

import com.allegro.backanonymization.config.JwtTokenProvider;
import com.allegro.backanonymization.dto.AnonymizationRequestDto;
import com.allegro.backanonymization.dto.DicomResponseDto.*;
import com.allegro.backanonymization.service.AnonymizationReceiveService;
import com.allegro.backanonymization.service.DicomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/api/dicom")
@RequiredArgsConstructor //의존성 주입
public class DicomController {
    private final DicomService dicomService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AnonymizationReceiveService anonymizationReceiveService;

//    //스터디 목록 불러오기
//    @GetMapping("/studies")
//    public ResponseEntity<List<StudyDto>> getStudies(
//            @RequestParam(required = false) String start,
//            @RequestParam(required = false) String end,
//            @RequestParam(required = false) String search
//    ) {
//        return ResponseEntity.ok(dicomService.getStudies(start, end, search));
//    }

    //시리즈 목록 불러오기
    @GetMapping("/series")
    public ResponseEntity<List<SeriesDto>> getSeries(
            @RequestParam(name = "study-key") Long studyKey
        ) {
        return ResponseEntity.ok(dicomService.getSeriesData(studyKey));
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

    // 원본 서버에서 받은 익명화 데이터 저장하기
    @PostMapping(value = "/get-anonymization")
    public ResponseEntity<Void> getAnonymization(@RequestBody List<AnonymizationRequestDto> request) {
        anonymizationReceiveService.saveStudies(request);
        return ResponseEntity.ok().build();
    }
}
