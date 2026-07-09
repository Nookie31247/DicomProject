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

/**
 * DICOM 관련 작업을 처리하는 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/medical/dicom")
@RequiredArgsConstructor
public class DicomController {
    private final DicomService dicomService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AiService aiService;

    //action -> 무슨 행동했는가? (조회/히든/다운)
    //tatgetType -> 무슨 종류의 대상인가? (환자/스터디/시리즈)
    //targetArgIndex -> 대상의 구체적인 번호(ID)가 메서드 파라미터중 멏 번째냐?를 가르킴. 안 적으면 기본값이 -1이라 -로 기록됨

    /**
     * 검색 조건을 기반으로 환자 목록을 검색합니다.
     *
     * @param token JWT 토큰
     * @param start 필터링을 위한 시작일
     * @param end 필터링을 위한 종료일
     * @param search 검색 키워드
     * @return 환자 목록
     */
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

    /**
     * 새로운 환자를 등록합니다.
     *
     * @param token JWT 토큰
     * @param requestDto 환자 등록 요청 데이터
     * @return 성공 메시지
     */
    @PostMapping("/patients")
    public ResponseEntity<String> addPatient(
            @CookieValue(name = "token") String token,
            @RequestBody PatientRequestDto requestDto) {

        Long doctorKey = jwtTokenProvider.getUserKey(token);
        dicomService.addPatient(doctorKey, requestDto);

        return ResponseEntity.ok("환자가 성공적으로 등록되었습니다.");
    }

    /**
     * 특정 환자의 검사(study) 목록을 검색합니다.
     *
     * @param token JWT 토큰
     * @param start 필터링을 위한 시작일
     * @param end 필터링을 위한 종료일
     * @param patientKey 환자의 고유 키
     * @param search 검색 키워드
     * @return 검사(study) 목록
     */
    //patientKey가 파라미터 4번째=인덱스3(patientkey)
    //api/medical/dicom/studies?patient-key=1 호출할 경우
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

    /**
     * 특정 검사(study)의 시리즈 목록을 검색합니다.
     *
     * @param token JWT 토큰
     * @param studyKey 검사(study)의 고유 키
     * @return 시리즈 목록
     */
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

    /**
     * 검사(study)에 대한 연구 허용 상태를 설정합니다.
     *
     * @param token JWT 토큰
     * @param request 업데이트할 검사(study) 목록
     * @return 성공 시 no content
     */
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

    /**
     * 환자에 대한 가시성(숨김/표시) 상태를 설정합니다.
     *
     * @param token JWT 토큰
     * @param request 업데이트할 환자 목록
     * @return 성공 시 no content
     */
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

    /**
     * 검사(study)에 대한 가시성(숨김/표시) 상태를 설정합니다.
     *
     * @param token JWT 토큰
     * @param request 업데이트할 검사(study) 목록
     * @return 성공 시 no content
     */
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

    /**
     * 시리즈에 대한 가시성(숨김/표시) 상태를 설정합니다.
     *
     * @param token JWT 토큰
     * @param request 업데이트할 시리즈 목록
     * @return 성공 시 no content
     */
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

    /**
     * 검사(study)를 ZIP 파일로 다운로드합니다.
     *
     * @param token JWT 토큰
     * @param studyKey 다운로드할 검사(study)의 고유 키
     * @return 압축된 검사(study) 파일
     */
    @AuditLogged(action = "DOWNLOAD", targetType = "STUDY", targetArgIndex = 0)
    @GetMapping("/studies/download")
    public ResponseEntity<StreamingResponseBody> downloadStudies(
            @CookieValue(name = "token") String token,
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
     * @param token JWT 토큰
     * @param seriesKey 다운로드할 시리즈의 고유 키
     * @return 압축된 시리즈 파일
     */
    @AuditLogged(action = "DOWNLOAD", targetType = "SERIES", targetArgIndex = 0)
    @GetMapping("/series/download")
    public ResponseEntity<StreamingResponseBody> downloadSeries(
            @CookieValue(name = "token") String token,
            @RequestParam("series-key") Long seriesKey
    ) {
        StreamingResponseBody stream = dicomService.downloadSeriesAsZip(seriesKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"series_" + seriesKey + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    /**
     * 여러 검사(study)와 시리즈를 단일 ZIP 파일로 다운로드합니다.
     *
     * @param token JWT 토큰
     * @param request 일괄 다운로드 요청
     * @return 압축된 파일들
     */
    // 여러 study/series 체크 후 한 번에 다운로드 — 요청 1번, zip 파일 1개로 응답
    // (studies/download, series/download를 체크 개수만큼 따로 부르면 브라우저가 다중 자동 다운로드를 막아버려서 추가함 그래서 하나의 zip으로 묶어서 처리)
    @AuditLogged(action = "DOWNLOAD", targetType = "BATCH")
    @PostMapping("/download/batch")
    public ResponseEntity<StreamingResponseBody> downloadBatch(
            @CookieValue(name = "token") String token,
            @RequestBody BatchDownloadDto request
    ) {
        StreamingResponseBody stream = dicomService.downloadBatchAsZip(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"download.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    /**
     * 환자를 위한 DICOM 파일을 업로드합니다.
     *
     * @param patientKey 환자의 고유 키
     * @param files 업로드할 파일 목록
     * @return 업로드 결과 요약
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResultDto> uploadFiles(
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

    /**
     * 뷰어 페이지를 위한 검사(study)의 상세 정보를 검색합니다.
     *
     * @param studyKey 검사(study)의 고유 키
     * @return 검사(study) 상세 정보
     */
    // Viewer 페이지에 띄우는 이름과 생년월일(getStudyDetail)
    @AuditLogged(action = "VIEW", targetType = "STUDY", targetArgIndex = 0)
    @GetMapping("/studies/{studyKey}")
    public ResponseEntity<DicomResponseDto.StudyDetailDto> getStudyDetail(@PathVariable Long studyKey) {
        return ResponseEntity.ok(aiService.getStudyDetail(studyKey));
    }

    /**
     * 뷰어 페이지에 표시하기 위해 검사(study)의 시리즈 목록을 검색합니다.
     *
     * @param studyKey 검사(study)의 고유 키
     * @return 시리즈 목록
     */
    // Viewer 페이지에 띄우는 series목록 (getSeriesByStudy) — 로그 안 남김
    // 슬라이스 넘길 때마다 호출돼서 row가 너무 많이 쌓임
    @GetMapping("/studies/{studyKey}/series")
    public ResponseEntity<List<DicomResponseDto.SeriesDto>> getSeriesOfStudy(@PathVariable Long studyKey) {
        return ResponseEntity.ok(aiService.getSeriesByStudy(studyKey));
    }

    /**
     * 시리즈의 인스턴스들을 검색합니다.
     *
     * @param seriesKey 시리즈의 고유 키
     * @return 인스턴스 목록
     */
    // series 정렬 (getInstancesOfSeries) — 로그 안 남김
    @GetMapping("/series/{seriesKey}/instances")
    public ResponseEntity<List<DicomResponseDto.InstanceInfoDto>> getInstancesOfSeries(@PathVariable("seriesKey") Long seriesKey) {
        return ResponseEntity.ok(aiService.getInstancesBySeries(seriesKey));
    }

    /**
     * 뷰어에 표시할 DICOM 인스턴스 파일을 검색합니다.
     *
     * @param seriesKey 시리즈의 고유 키
     * @param instanceId 인스턴스의 ID
     * @return DICOM 파일의 스트리밍 응답 본문
     */
    // Viewer에서 Dicom 이미지를 띄우는 기능 (getInstanceFile) — 로그 안 남김
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
