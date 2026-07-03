package com.allegro.dicomback.service;

import com.allegro.dicomback.dto.PatientDto;
import com.allegro.dicomback.dto.StudyAllocationDto;
import com.allegro.dicomback.entity.*;
import com.allegro.dicomback.entity.user.User;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklistService {

    private final DoctorWorklistRepository doctorWorklistRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final StudyRepository studyRepository;

    // 로그인한 의사에게 등록되지 않은 환자 리스트를 찾아 반환
    public List<Patient> findUnassignedPatients(Long userKey, String keyword) {
        // 1. 이름이나 ID로 환자 검색 (keyword를 pId 혹은 pName에 활용)
        // 여기서는 간단하게 이름 검색을 예시로 듭니다.
        List<Patient> allPatients = patientRepository.findBypName(keyword);

        if(allPatients.isEmpty()) {
            patientRepository.findById(keyword).ifPresent(allPatients::add);
        }

        // 2. 검색된 환자 중, 해당 의사가 이미 담당 중인 환자는 제외
        return allPatients.stream()
                .filter(patient -> !doctorWorklistRepository.existsByDoctor_UserKeyAndPatient_pId(userKey, patient.getPId()))
                .toList();
    }

    // findUnassignedPatients()로 나온 환자를 의사가 추가하는 기능
    @Transactional
    public void addPatientToWorklist(Long userKey, String pId) {
        // 1. 이미 등록된 환자인지 확인 (중복 등록 방지)
        boolean exists = doctorWorklistRepository.existsByDoctor_UserKeyAndPatient_pId(userKey, pId);

        if (!exists) {
            // 2. 의사와 환자 엔티티 조회
            User doctor = userRepository.findById(userKey)
                    .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
            Patient patient = patientRepository.findById(pId)
                    .orElseThrow(() -> new BaseException(ErrorCode.PATIENT_NOT_FOUND));

            // 3. 엔티티 생성 및 저장
            doctorWorklistRepository.save(DoctorWorklist.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .study(null) // 초기에는 검사 없이 환자만 등록
                    .build());
        }
    }

    // 로그인한 의사가 담당하는 환자 리스트를 반환
    public List<PatientDto> getPatientsByDoctor(Long userKey) {
        return doctorWorklistRepository.findByDoctor_UserKey(userKey)
                .stream().map(worklist -> PatientDto.fromEntity(worklist.getPatient()))
                .toList();
    }


//    // 1. 환자 등록 (의사 개인의 작업 목록에 환자 추가)
//    @Transactional
//    public void addPatientToWorklist(Long userKey, String pId) {
//        // 이미 등록된 환자인지 확인 (중복 방지)
//        boolean exists = worklistRepository.existsByDoctor_UserKeyAndPatient_PIdAndStudyIsNull(userKey, pId);
//        if (!exists) {
//            User doctor = userRepository.findById(userKey).orElseThrow();
//            Patient patient = patientRepository.findById(pId).orElseThrow();
//
//            worklistRepository.save(DoctorWorklist.builder()
//                    .doctor(doctor)
//                    .patient(patient)
//                    .build());
//        }
//    }
//
//    // 2. 환자의 전체 검사 목록 조회 (프론트엔드 추가 모달용)
//    public List<Study> getStudiesByPatient(String pId) {
//        return studyRepository.findByPatient_PId(pId);
//    }
//
//    // 3. 특정 환자에 검사들 할당 (기존 데이터 업데이트 또는 신규 추가)
//    @Transactional
//    public void assignStudiesToPatient(Long userKey, String pId, List<Long> studyKeys) {
//        for (Long studyKey : studyKeys) {
//            // 이미 할당된 검사인지 확인
//            if (!worklistRepository.existsByDoctor_UserKeyAndPatient_PIdAndStudy_StudyKey(userKey, pId, studyKey)) {
//                User doctor = userRepository.findById(userKey).orElseThrow();
//                Patient patient = patientRepository.findById(pId).orElseThrow();
//                Study study = studyRepository.findById(studyKey).orElseThrow();
//
//                worklistRepository.save(DoctorWorklist.builder()
//                        .doctor(doctor)
//                        .patient(patient)
//                        .study(study)
//                        .build());
//            }
//        }
//    }
//
//    // 4. 의사의 환자 목록 조회
//    public List<Patient> getPatientsByDoctor(Long userKey) {
//        return worklistRepository.findPatientsByDoctor(userKey);
//    }
//
//    // [보너스] 검사 목록을 가져올 때 할당 여부까지 확인
//    // 특정 환자에 대한 검사 목록 + 할당 상태 조회
//    public List<StudyAllocationDto> getStudiesWithAllocationStatus(Long userKey, String pId) {
//        // 1. 해당 환자의 전체 검사 목록
//        List<Study> allStudies = studyRepository.findByPatient_PId(pId);
//
//        // 2. 내가 이미 할당해 둔 검사 키(StudyKey) 목록만 추출
//        List<Long> assignedStudyKeys = worklistRepository.findByDoctor_UserKeyAndPatient_PId(userKey, pId)
//                .stream()
//                .filter(worklist -> worklist.getStudy() != null)
//                .map(worklist -> worklist.getStudy().getStudyKey())
//                .toList();
//
//        // 3. 전체 목록에 할당 여부를 체크하여 DTO 변환
//        return allStudies.stream()
//                .map(study -> StudyAllocationDto.fromEntity(
//                        study,
//                        assignedStudyKeys.contains(study.getStudyKey())
//                ))
//                .toList();
//    }
}