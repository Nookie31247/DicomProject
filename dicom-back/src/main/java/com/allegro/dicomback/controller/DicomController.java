package com.allegro.dicomback.controller;

import com.allegro.dicomback.config.JwtTokenProvider;
import com.allegro.dicomback.dto.DicomRequestDto.*;
import com.allegro.dicomback.dto.DicomResponseDto;
import com.allegro.dicomback.dto.DicomResponseDto.*;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.service.AiService;
import com.allegro.dicomback.service.DicomService;
import lombok.extern.slf4j.Slf4j;
//import com.allegro.dicomback.service.OrthancSyncService;
import com.allegro.dicomback.log.AuditLogged;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dicom")
@RequiredArgsConstructor //의존성 주입
public class DicomController {
    private final DicomService dicomService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AiService aiService;

    //action -> 무슨 행동했는가? (조회/히든/다운)
    //tatgetType -> 무슨 종류의 대상인가? (환자/스터디/시리즈)
    //targetArgIndex -> 대상의 구체적인 번호(ID)가 메서드 파라미터중 멏 번째냐?를 가르킴. 안 적으면 기본값이 -1이라 -로 기록됨

    //환자 목록 불러오기
    //Log: ActionType=SEARCH, TargetType=PATIENT, TargetUID=- (환자 목록 검색은 특정 환자 하나가 아니라서 대상 없음)
    @AuditLogged(action = "SEARCH", targetType = "PATIENT")
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
    //patientKey가 파라미터 4번째=인덱스3(patientkey)
    //api/dicom/studies?patient-key=1 호출할 경우
    //Log: ActionType=SEARCH, TargetType=STUDY, TargetUID=1 (몇 번 환자의 검사 목록을 열람하였는지)
    @AuditLogged(action = "SEARCH", targetType = "STUDY", targetArgIndex = 3)
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
    //studyKey가 파라미터 2번째 = 인덱스 1
    @AuditLogged(action = "SEARCH", targetType = "SERIES", targetArgIndex = 1)
    @GetMapping("/series")
    public ResponseEntity<List<SeriesDto>> getSeries(
            @CookieValue(name = "token") String token,
            @RequestParam(name = "study-key") Long studyKey
    ) {
        Long doctorKey = jwtTokenProvider.getUserKey(token);
        return ResponseEntity.ok(dicomService.getSeries(doctorKey, studyKey));
    }

    // 스터디 연구 목적 활용 허용 설정
    @AuditLogged(action = "RESEARCH_ALLOW", targetType = "STUDY")
    @PostMapping("/studies/research-allow")
    public ResponseEntity<Void> setStudiesResearchAllow(
            @CookieValue(name = "token") String token,
            @RequestBody List<StudyResearchDto> request
    ) {
        Long doctorKey = jwtTokenProvider.getUserKey(token);
        dicomService.setAllowResearchStudies(doctorKey, request);
        return ResponseEntity.noContent().build();
    }

    //환자 목록 숨기기/보이기 설정
    @AuditLogged(action = "HIDE", targetType = "PATIENT")
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
    @AuditLogged(action = "HIDE", targetType = "STUDY")
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
    @AuditLogged(action = "HIDE", targetType = "SERIES")
    @PostMapping("/series/hide")
    public ResponseEntity<Void> hideSeries(
            @CookieValue(name = "token") String token,
            @RequestBody List<SeriesHideDto> request
    ) {
        Long doctorKey = jwtTokenProvider.getUserKey(token);
        dicomService.setHideSeries(doctorKey, request);
        return ResponseEntity.noContent().build();
    }

    // 연구원 계정의 다운로드를 막는 임시 안전장치.
    // 지금 다운로드는 비식별화 안 된 원본 DICOM을 그대로 zip으로 내려주는데,
    // 연구원은 원칙상 익명화된 데이터만 받아야 함. 익명화 기능(#11)이 아직 없으므로
    // 그게 붙기 전까지는 연구원의 다운로드 요청 자체를 서버에서 403으로 막는다.
    private void blockResearcherDownload(String token) {
        String userType = jwtTokenProvider.getUserType(token);
        if ("RESEARCHER".equals(userType)) {
            throw new BaseException(ErrorCode.RESEARCHER_DOWNLOAD_NOT_READY);
        }
    }

    //스터디 다운로드
    @AuditLogged(action = "DOWNLOAD", targetType = "STUDY", targetArgIndex = 0)
    @GetMapping("/studies/download")
    public ResponseEntity<StreamingResponseBody> downloadStudies(
            @CookieValue(name = "token") String token,
            @RequestParam("study-key") Long studyKey
    ) {
        blockResearcherDownload(token);
        StreamingResponseBody stream = dicomService.downloadStudyAsZip(studyKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"study_" + studyKey + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    //시리즈 다운로드
    @AuditLogged(action = "DOWNLOAD", targetType = "SERIES", targetArgIndex = 0)
    @GetMapping("/series/download")
    public ResponseEntity<StreamingResponseBody> downloadSeries(
            @CookieValue(name = "token") String token,
            @RequestParam("series-key") Long seriesKey
    ) {
        blockResearcherDownload(token);
        StreamingResponseBody stream = dicomService.downloadSeriesAsZip(seriesKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"series_" + seriesKey + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    // 여러 study/series 체크 후 한 번에 다운로드 — 요청 1번, zip 파일 1개로 응답
    // (studies/download, series/download를 체크 개수만큼 따로 부르면 브라우저가 다중 자동 다운로드를 막아버려서 추가함 그래서 하나의 zip으로 묶어서 처리)
    @AuditLogged(action = "DOWNLOAD", targetType = "BATCH")
    @PostMapping("/download/batch")
    public ResponseEntity<StreamingResponseBody> downloadBatch(
            @CookieValue(name = "token") String token,
            @RequestBody BatchDownloadDto request
    ) {
        blockResearcherDownload(token);
        StreamingResponseBody stream = dicomService.downloadBatchAsZip(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"download.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }
    //다운로드 페이지
    @GetMapping("/studies/research")
    public ResponseEntity<List<StudyDto>> getResearchStudies(@CookieValue(name = "token") String token) {
        jwtTokenProvider.getUserKey(token);
        return ResponseEntity.ok(dicomService.getResearchStudies());
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResultDto> uploadFiles( // 응답을 문자열 대신 결과 요약 DTO로 변경
            @RequestParam("patientKey") Long patientKey,
            @RequestParam("files") List<MultipartFile> files) {

        List<String> succeeded = new ArrayList<>();   // 성공한 파일명
        List<String> failed = new ArrayList<>();      // 실패한 파일명

        for (MultipartFile file : files) {
            try {
                dicomService.processDicomFile(patientKey, file);
                succeeded.add(file.getOriginalFilename());
            } catch (Exception e) {
                // 여기서 return 하지 않고 로그만 남긴 뒤 다음 파일로 계속 진행한다
                log.error("파일 처리 실패: {}", file.getOriginalFilename(), e);
                failed.add(file.getOriginalFilename());
            }
        }
        // 몇 개 성공/실패했는지 프론트에서 바로 보여줄 수 있게 요약해서 반환
        return ResponseEntity.ok(new UploadResultDto(succeeded, failed));
    }

    //Viewer 페이지에 띄우는 이름과 생년월일(getStudyDetail)
    @AuditLogged(action = "VIEW", targetType = "STUDY", targetArgIndex = 0)
    @GetMapping("/studies/{studyKey}")
    public ResponseEntity<DicomResponseDto.StudyDetailDto> getStudyDetail(@PathVariable Long studyKey) {
        return ResponseEntity.ok(aiService.getStudyDetail(studyKey));
    }

    //Viewer 페이지에 띄우는 series목록 (getSeriesByStudy) — 로그 안 남김
    //슬라이스 넘길 때마다 호출돼서 row가 너무 많이 쌓임
    @GetMapping("/studies/{studyKey}/series")
    public ResponseEntity<List<DicomResponseDto.SeriesDto>> getSeriesOfStudy(@PathVariable Long studyKey) {
        return ResponseEntity.ok(aiService.getSeriesByStudy(studyKey));
    }

    //series 정렬 (getInstancesOfSeries) — 로그 안 남김
    @GetMapping("/series/{seriesKey}/instances")
    public ResponseEntity<List<DicomResponseDto.InstanceInfoDto>> getInstancesOfSeries(@PathVariable("seriesKey") Long seriesKey) {
        return ResponseEntity.ok(aiService.getInstancesBySeries(seriesKey));
    }

    //Viewer에서 Dicom 이미지를 띄우는 기능 (getInstanceFile) — 로그 안 남기
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
