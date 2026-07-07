package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
        where p.doctorKey.key = :doctorKey
          and p.key = :patientKey
          and s.createdAt between :start and :end
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
        where p.doctorKey.key = :doctorKey
          and p.key = :patientKey
          and s.createdAt between :start and :end
          and lower(coalesce(s.description, '')) like lower(concat('%', :search, '%'))
        """)
    List<Study> findStudiesWithSearch(
            @Param("doctorKey") Long doctorKey,
            @Param("patientKey") Long patientKey,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("search") String search
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
                update Study s
                set s.hiddenFlag = :isHidden
                where s.key in :studyKeys
                and exists(
                    select 1 from Patient p
                    where p = s.patientKey
                    and p.doctorKey.key = :doctorKey
                )
            """)
    int changeHiddenFlag(
            @Param("doctorKey") Long doctorKey,
            @Param("studyKeys") List<Long> studyKeys,
            @Param("isHidden") boolean isHidden
    );

    // 담당 의사의 환자들 중 연구 활용 허용된 스터디 전체 조회
    @Query("""
    select s
    from Study s
    join s.patientKey p
    where p.doctorKey.key = :doctorKey
      and s.allowResearch = true
      and s.hiddenFlag = false
    order by s.createdAt desc
    """)
    List<Study> findResearchStudies(@Param("doctorKey") Long doctorKey);

}
