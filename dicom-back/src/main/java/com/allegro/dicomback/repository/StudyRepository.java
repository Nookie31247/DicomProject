package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // DICOM 고유 UID로 단건 검사 조회 (Patient까지 한 번에 조인해서 가져옴)
    @Query("SELECT s FROM Study s JOIN FETCH s.patient WHERE s.studyInstanceUid = :studyInstanceUid")
    Optional<Study> findByStudyInstanceUid(@Param("studyInstanceUid") String studyInstanceUid);
}