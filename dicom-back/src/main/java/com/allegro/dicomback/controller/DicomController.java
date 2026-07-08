package com.allegro.dicomback.controller;

import com.allegro.dicomback.config.JwtTokenProvider;
import com.allegro.dicomback.dto.DicomRequestDto.*;
import com.allegro.dicomback.dto.DicomResponseDto;
import com.allegro.dicomback.dto.DicomResponseDto.*;
import com.allegro.dicomback.service.AiService;
import com.allegro.dicomback.service.DicomService;
//import com.allegro.dicomback.service.OrthancSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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
    private final AiService aiService;

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

    @PostMapping("/patients")
    public ResponseEntity<String> addPatient(
            @CookieValue(name = "token") String token,
            @RequestBody PatientRequestDto requestDto) {

        Long doctorKey = jwtTokenProvider.getUserKey(token);
        dicomService.addPatient(doctorKey, requestDto);

        return ResponseEntity.ok("환자가 성공적으로 등록되었습니다.");
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
    //다운로드 페이지
    @GetMapping("/studies/research")
    public ResponseEntity<List<StudyDto>> getResearchStudies(@CookieValue(name = "token") String token) {
//        Long doctorKey = jwtTokenProvider.getUserKey(token);
        jwtTokenProvider.getUserKey(token);
        return ResponseEntity.ok(dicomService.getResearchStudies());
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFiles(
            @RequestParam("patientKey") Long patientKey,
            @RequestParam("files") List<MultipartFile> files) {

        for (MultipartFile file : files) {
            try {
                // 아까 작성한 서비스 메서드 호출
                dicomService.processDicomFile(patientKey, file);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("파일 처리 실패: " + file.getOriginalFilename());
            }
        }
        return ResponseEntity.ok("업로드 완료");
    }

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

    //Viewer 페이지에 띄우는 이름과 생년월일(getStudyDetail)
    @GetMapping("/studies/{studyKey}")
    public ResponseEntity<DicomResponseDto.StudyDetailDto> getStudyDetail(@PathVariable Long studyKey) {
        return ResponseEntity.ok(aiService.getStudyDetail(studyKey));
    }

    //Viewer 페이지에 띄우는 series목록 해당 기능을 통해서 왼쪽에 해당 환자의 검사기록(series)을 조회 (getSeriesByStudy)
    @GetMapping("/studies/{studyKey}/series")
    public ResponseEntity<List<DicomResponseDto.SeriesDto>> getSeriesOfStudy(@PathVariable Long studyKey) {
        return ResponseEntity.ok(aiService.getSeriesByStudy(studyKey));
    }

    //series 정렬 - 처음가져왔을때 해시값을 통해서 가져오므로 정렬이 제대로 안되는 경우가 있기에 정렬 작업이 필요 (getInstancesOfSeries)
    @GetMapping("/series/{seriesKey}/instances")
    public ResponseEntity<List<String>> getInstancesOfSeries(@PathVariable("seriesKey") Long seriesKey) {
        return ResponseEntity.ok(aiService.getInstanceIdsBySeries(seriesKey));
    }

    //Viewer 페이지에서 Dicom 이미지를 띄우는 기능 series에 있는 정렬된 Dicom이미지를 화면에 출력한다.(getInstanceFile)
    @GetMapping("/series/{seriesKey}/instances/{instanceId}/file")
    public ResponseEntity<StreamingResponseBody> getInstanceFile(
            @PathVariable Long seriesKey,
            @PathVariable String instanceId
    ) {
        StreamingResponseBody stream = aiService.getInstanceFile(seriesKey, instanceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/dicom"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + instanceId + ".dcm\"")
                .body(stream);
    }
}
