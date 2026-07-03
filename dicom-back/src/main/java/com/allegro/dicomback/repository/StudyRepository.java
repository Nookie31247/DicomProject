package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface StudyRepository extends JpaRepository<Study, Long> {
    // 환자 외래키, 시작일, 종료일로 찾을 때
    List<Study> findByPatient_KeyAndStudyDateTimeBetween (
            Long patientKey,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // 환자 외래키, 시작일, 종료일, 설명을 검색으로 찾을 때
    List<Study> findByPatient_KeyAndStudyDateTimeBetweenAndDescriptionContaining (
        Long patientKey,
        LocalDateTime start,
        LocalDateTime end,
        String description
    );
}
