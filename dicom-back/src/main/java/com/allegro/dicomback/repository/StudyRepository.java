package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StudyRepository extends JpaRepository<Study, Long> {
    // 검색어가 없을 때의 조회
    @Query("""
        select s
        from Study s
        join s.patientKey p
        where p.doctor.key = :doctorKey
          and p.key = :patientKey
          and s.studyDateTime between :start and :end
        """)
    List<Study> findStudiesWithoutSearch(
            @Param("doctorKey") Long doctorKey,
            @Param("patientKey") Long patientKey,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


    // 검색어가 있을 때의 조회
    @Query("""
        select s
        from Study s
        join s.patientKey p
        where p.doctor.key = :doctorKey
          and p.key = :patientKey
          and s.studyDateTime between :start and :end
          and lower(coalesce(s.description, '')) like lower(concat('%', :search, '%'))
        """)
    List<Study> findStudiesWithSearch(
            @Param("doctorKey") Long doctorKey,
            @Param("patientKey") Long patientKey,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("search") String search
    );

    Long patient(Patient patient);
}
