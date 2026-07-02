package com.allegro.dicomback.controller;

import com.allegro.dicomback.config.JwtTokenProvider;
import com.allegro.dicomback.dto.StudyAllocationDto;
import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.user.User;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.UserRepository;
import com.allegro.dicomback.service.WorklistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/worklist")
@RequiredArgsConstructor
@Slf4j
public class WorklistController {

    private final WorklistService worklistService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 5. [신규 추가] 의사가 담당하지 않은 환자 검색 (이름 또는 ID 기준)
    @GetMapping("/patients/search")
    public ResponseEntity<List<Patient>> searchUnassignedPatients(
            @AuthenticationPrincipal String userId,
            @RequestParam String keyword) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        // 1. 토큰에서 userKey 추출
//        String token = request.getHeader("Authorization").substring(7);
//        Long userKey = jwtTokenProvider.getUserKey(token);

        // 2. 서비스 호출
        List<Patient> result = worklistService.findUnassignedPatients(user.getUserKey(), keyword);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/patients/add")
    public ResponseEntity<String> addPatient(
            @AuthenticationPrincipal String userId,
            @RequestParam String pId) {

        // 1. userId로 userKey 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        // 2. 서비스 호출
        worklistService.addPatientToWorklist(user.getUserKey(), pId);

        return ResponseEntity.ok("환자가 성공적으로 추가되었습니다.");
    }

//    // 1. 의사가 담당 환자 목록 불러오기
//    @GetMapping("/patients")
//    public ResponseEntity<List<Patient>> getMyPatients(HttpServletRequest request) {
//
//        String token = request.getHeader("Authorization").substring(7);
//        Long userKey = jwtTokenProvider.getUserKey(token);
//
//        return ResponseEntity.ok(worklistService.getPatientsByDoctor(userKey));
//    }
//
//    // 2. 환자 등록 (내 목록에 추가)
//    @PostMapping("/patients")
//    public ResponseEntity<Void> addPatient(HttpServletRequest request,
//                                           @RequestParam String pId) {
//        String token = request.getHeader("Authorization").substring(7);
//        Long userKey = jwtTokenProvider.getUserKey(token);
//
//        log.info("UserKey: {}", userKey);
//        log.info("PId: {}", pId);
//
//        worklistService.addPatientToWorklist(userKey, pId);
//        return ResponseEntity.ok().build();
//    }
//
//    // 3. 특정 환자의 검사 목록 및 할당 상태 조회
//    @GetMapping("/studies")
//    public ResponseEntity<List<StudyAllocationDto>> getStudiesWithStatus(
//            HttpServletRequest request,
//            @RequestParam String pId) {
//
//        String token = request.getHeader("Authorization").substring(7);
//        Long userKey = jwtTokenProvider.getUserKey(token);
//
//        return ResponseEntity.ok(worklistService.getStudiesWithAllocationStatus(userKey, pId));
//    }
//
//    // 4. 특정 검사들을 내 담당으로 할당하기
//    @PostMapping("/studies/assign")
//    public ResponseEntity<Void> assignStudies(
//            HttpServletRequest request,
//            @RequestParam String pId,
//            @RequestBody List<Long> studyKeys) { // 검사 키 리스트를 JSON 배열로 받음
//
//        String token = request.getHeader("Authorization").substring(7);
//        Long userKey = jwtTokenProvider.getUserKey(token);
//
//        worklistService.assignStudiesToPatient(userKey, pId, studyKeys);
//        return ResponseEntity.ok().build();
//    }
}