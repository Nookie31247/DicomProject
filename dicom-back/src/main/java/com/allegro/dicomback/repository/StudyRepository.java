package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long> {

    // 특정 환자의 검사 목록 조회 (정상 데이터만)
    List<Study> findByPatient_pIdAndDelFlag(String pId, Integer delFlag);

    // 검사 설명(Description) 검색 (정상 데이터만)
    List<Study> findByDescriptionContainingAndDelFlag(String description, Integer delFlag);

    // 검사 날짜 기간 검색
    List<Study> findByStudyDateTimeBetweenAndDelFlag(LocalDateTime start, LocalDateTime end, Integer delFlag);

    // DICOM 고유 UID로 단건 검사 조회
    Optional<Study> findByStudyInstanceUid(String studyInstanceUID);

    // 검사(Study)와 연결된 PId(환자 고유 번호)를 기반으로 환자 정보 리스트(List<Study>) 조회.
//    List<Study> findByPatient_PId(String pId);
}
