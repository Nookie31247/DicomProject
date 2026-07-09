package com.allegro.backanonymization.controller;

import com.allegro.backanonymization.dto.AnonymizationRequestDto;
import com.allegro.backanonymization.dto.DicomRequestDto.BatchDownloadDto;
import com.allegro.backanonymization.dto.DicomResponseDto.InstanceInfoDto;
import com.allegro.backanonymization.dto.DicomResponseDto.SeriesDto;
import com.allegro.backanonymization.dto.DicomResponseDto.StudyDto;
import com.allegro.backanonymization.service.AnonymizationReceiveService;
import com.allegro.backanonymization.service.DicomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

/**
 * DICOM 관련 엔드포인트를 위한 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/research/dicom")
@RequiredArgsConstructor
public class DicomController {

    private final DicomService dicomService;
    private final AnonymizationReceiveService anonymizationReceiveService;

    /**
     * 연구 목록을 가져옵니다.
     *
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param search 검색어
     * @return 연구 목록
     */
    @GetMapping("/studies")
    public ResponseEntity<List<StudyDto>> getStudies(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(dicomService.getStudiesData(start, end, search));
    }

    /**
     * 리서치 연구 목록을 가져옵니다.
     *
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param search 검색어
     * @return 리서치 연구 목록
     */
    @GetMapping("/studies/research")
    public ResponseEntity<List<StudyDto>> getResearchStudies(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(dicomService.getStudiesData(start, end, search));
    }

    /**
     * 특정 연구에 대한 시리즈를 가져옵니다.
     *
     * @param studyKey 연구 키
     * @return 시리즈 목록
     */
    @GetMapping("/series")
    public ResponseEntity<List<SeriesDto>> getSeries(
            @RequestParam(name = "study-key") Long studyKey
    ) {
        return ResponseEntity.ok(dicomService.getSeriesData(studyKey));
    }

    /**
     * 특정 연구에 대한 시리즈를 가져옵니다.
     *
     * @param studyKey 연구 키
     * @return 시리즈 목록
     */
    @GetMapping("/studies/{studyKey}/series")
    public ResponseEntity<List<SeriesDto>> getStudySeries(@PathVariable Long studyKey) {
        return ResponseEntity.ok(dicomService.getSeriesData(studyKey));
    }

    /**
     * 연구를 ZIP 파일로 다운로드합니다.
     *
     * @param studyKey 연구 키
     * @return ZIP 파일을 포함하는 스트리밍 응답 본문
     */
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

    /**
     * 시리즈를 ZIP 파일로 다운로드합니다.
     *
     * @param seriesKey 시리즈 키
     * @return ZIP 파일을 포함하는 스트리밍 응답 본문
     */
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

    /**
     * 연구 또는 시리즈의 배치를 ZIP 파일로 다운로드합니다.
     *
     * @param request 배치 다운로드 요청
     * @return ZIP 파일을 포함하는 스트리밍 응답 본문
     */
    @PostMapping("/download/batch")
    public ResponseEntity<StreamingResponseBody> downloadBatch(@RequestBody BatchDownloadDto request) {
        StreamingResponseBody stream = dicomService.downloadBatchAsZip(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"research_dicom.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    /**
     * 특정 시리즈에 대한 인스턴스를 가져옵니다.
     *
     * @param seriesKey 시리즈 키
     * @return 인스턴스 목록
     */
    @GetMapping("/series/{seriesKey}/instances")
    public ResponseEntity<List<InstanceInfoDto>> getInstances(@PathVariable Long seriesKey) {
        return ResponseEntity.ok(dicomService.getInstancesBySeries(seriesKey));
    }

    /**
     * 특정 인스턴스 파일을 가져옵니다.
     *
     * @param seriesKey 시리즈 키
     * @param instanceId 인스턴스 ID
     * @return DICOM 파일을 포함하는 스트리밍 응답 본문
     */
    @GetMapping("/series/{seriesKey}/instances/{instanceId}/file")
    public ResponseEntity<StreamingResponseBody> getInstanceFile(
            @PathVariable Long seriesKey,
            @PathVariable String instanceId
    ) {
        StreamingResponseBody stream = dicomService.getInstanceFile(seriesKey, instanceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/dicom"))
                .body(stream);
    }

    /**
     * 요청 목록에 대한 익명화를 시작합니다.
     *
     * @param request 익명화 요청 목록
     * @return 빈 ResponseEntity
     */
    @PostMapping("/get-anonymization")
    public ResponseEntity<Void> getAnonymization(@RequestBody List<AnonymizationRequestDto> request) {
        anonymizationReceiveService.saveStudies(request);
        return ResponseEntity.ok().build();
    }
}
