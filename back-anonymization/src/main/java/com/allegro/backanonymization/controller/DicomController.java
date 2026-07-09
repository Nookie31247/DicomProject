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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/api/research/dicom")
@RequiredArgsConstructor
public class DicomController {

    private final DicomService dicomService;
    private final AnonymizationReceiveService anonymizationReceiveService;

    @GetMapping("/studies")
    public ResponseEntity<List<StudyDto>> getStudies(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(dicomService.getStudiesData(start, end, search));
    }

    @GetMapping("/studies/research")
    public ResponseEntity<List<StudyDto>> getResearchStudies(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(dicomService.getStudiesData(start, end, search));
    }

    @GetMapping("/series")
    public ResponseEntity<List<SeriesDto>> getSeries(
            @RequestParam(name = "study-key") Long studyKey
    ) {
        return ResponseEntity.ok(dicomService.getSeriesData(studyKey));
    }

    @GetMapping("/studies/{studyKey}/series")
    public ResponseEntity<List<SeriesDto>> getStudySeries(@PathVariable Long studyKey) {
        return ResponseEntity.ok(dicomService.getSeriesData(studyKey));
    }

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

    @PostMapping("/download/batch")
    public ResponseEntity<StreamingResponseBody> downloadBatch(@RequestBody BatchDownloadDto request) {
        StreamingResponseBody stream = dicomService.downloadBatchAsZip(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"research_dicom.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    @GetMapping("/series/{seriesKey}/instances")
    public ResponseEntity<List<InstanceInfoDto>> getInstances(@PathVariable Long seriesKey) {
        return ResponseEntity.ok(dicomService.getInstancesBySeries(seriesKey));
    }

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

    @PostMapping("/get-anonymization")
    public ResponseEntity<Void> getAnonymization(@RequestBody List<AnonymizationRequestDto> request) {
        anonymizationReceiveService.saveStudies(request);
        return ResponseEntity.ok().build();
    }
}
