package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.DoctorWorklist;
import com.allegro.dicomback.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DoctorWorklistRepository extends JpaRepository<DoctorWorklist, Long> {
//    // 1. 특정 의사가 담당하는 '환자 목록' 추출 (중복 제거)
//    List<Patient> findByDoctor_UserKey(Long userKey);
//
//    // 2. 특정 의사와 환자 사이의 작업 기록 조회 (검사 할당 시 사용)
//    List<DoctorWorklist> findByDoctor_UserKeyAndPatient_PId(Long userKey, String pId);
//
//    // 3. 특정 환자의 검사 할당 여부 확인
//    boolean existsByDoctor_UserKeyAndPatient_PIdAndStudy_StudyKey(Long userKey, String pId, Long studyKey);
//
//    // 4. 특정 의사가 담당하는 환자의 '할당된 검사 기록'만 조회(Study가 null이 아닌 경우만 필터링)
//    List<DoctorWorklist> findAssignedStudiesByDoctorAndPatient(@Param("userKey") Long userKey, @Param("pId") String pId);
//
//    // 환자 최초 등록 시 중복 방지를 위한 메서드
//    // 의사와 환자는 매핑되어 있는데, 아직 검사가 할당되지 않은(null) 행이 있는지 확인
//    boolean existsByDoctor_UserKeyAndPatient_PIdAndStudyIsNull(Long userKey, String pId);

    // 의사의 UserKey를 통해 DoctorWorklist를 찾아 그 안의 Patient를 리스트로 반환
    List<DoctorWorklist> findByDoctor_UserKey(Long userKey);

    // 특정 의사가 해당 환자를 담당하고 있는지 확인
    boolean existsByDoctor_UserKeyAndPatient_pId(Long userKey, String pId);
}