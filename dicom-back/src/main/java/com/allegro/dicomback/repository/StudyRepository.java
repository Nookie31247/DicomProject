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

    // 같은 Study Instance UID가 이미 저장되어 있는지 확인 (중복 저장 방지)
    boolean existsByUid(String uid);

    // uid(Study Instance UID)로 기존 Study를 찾기 (있으면 재사용, 없으면 신규 생성)
    java.util.Optional<Study> findByUid(String uid);

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

    // 연구 목적 활용 허용 여부(allowResearch) 변경
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
                update Study s
                set s.allowResearch = :isAllowed
                where s.key in :studyKeys
                and exists(
                    select 1 from Patient p
                    where p = s.patientKey
                    and p.doctorKey.key = :doctorKey
                )
            """)
    int changeAllowResearch(
            @Param("doctorKey") Long doctorKey,
            @Param("studyKeys") List<Long> studyKeys,
            @Param("isAllowed") boolean isAllowed
    );

    // 담당 의사의 환자들 중 연구 활용 허용된 스터디 전체 조회
    @Query("""
    select s
    from Study s
    join fetch s.patientKey p
    where s.allowResearch = true
      and s.hiddenFlag = false
    order by s.createdAt desc
    """)
    List<Study> findResearchStudies();

}
// 담당 의사와 무관하게, 연구 활용 허용된(allowResearch=true) 스터디 전체 조회
//@Query("""
//    select s
//    from Study s
//    join fetch s.patientKey p
//    where s.allowResearch = true
//      and s.hiddenFlag = false
//    order by s.createdAt desc
//    """)
//List<Study> findResearchStudies();

