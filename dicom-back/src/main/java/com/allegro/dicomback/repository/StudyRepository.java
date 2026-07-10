package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link Study} 엔티티를 위한 레포지토리 인터페이스입니다.
 * 표준 CRUD 및 사용자 정의 쿼리 작업을 제공하기 위해 JpaRepository를 확장합니다.
 */
public interface StudyRepository extends JpaRepository<Study, Long> {

    /**
     * 주어진 Study Instance UID를 가진 검사(study)가 존재하는지 확인합니다.
     * 중복 저장을 방지하는 데 유용합니다.
     *
     * @param uid Study Instance UID
     * @return 검사(study)가 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByUid(String uid);

    /**
     * Study Instance UID로 검사(study)를 찾습니다. 기존 검사(study)를 재사용하는 데 사용할 수 있습니다.
     *
     * @param uid Study Instance UID
     * @return 찾은 경우 {@link Study}를 포함하는 {@link java.util.Optional}, 그렇지 않으면 비어 있음
     */
    java.util.Optional<Study> findByUid(String uid);

    /**
     * 텍스트 검색 없이 날짜 범위 내에서 특정 의사와 환자에 대한 검사(study)를 검색합니다.
     *
     * @param doctorKey 의사 키
     * @param patientKey 환자 키
     * @param start 범위의 시작 날짜
     * @param end 범위의 종료 날짜
     * @return 조건과 일치하는 {@link Study} 목록
     */
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

    /**
     * 특정 의사와 환자에 대해 날짜 범위 내에서 검사(study) 설명의 검색어로 필터링하여 검사(study)를 검색합니다.
     *
     * @param doctorKey 의사 키
     * @param patientKey 환자 키
     * @param start 범위의 시작 날짜
     * @param end 범위의 종료 날짜
     * @param search 설명에서 찾을 검색어
     * @return 조건과 일치하는 {@link Study} 목록
     */
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

    /**
     * 특정 의사의 환자에 속하는 여러 검사(study)에 대한 숨김 플래그를 업데이트합니다.
     *
     * @param doctorKey 의사 키
     * @param studyKeys 업데이트할 검사(study) 키 목록
     * @param isHidden 새로운 숨김 플래그 상태
     * @return 업데이트된 레코드 수
     */
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

    /**
     * 특정 의사의 환자에 속하는 여러 검사(study)에 대한 연구 허용 플래그를 업데이트합니다.
     *
     * @param doctorKey 의사 키
     * @param studyKeys 업데이트할 검사(study) 키 목록
     * @param isAllowed 새로운 연구 허용 상태
     * @return 업데이트된 레코드 수
     */
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

    /**
     * 연구 목적으로 허용되고 숨겨지지 않은 모든 검사(study)를 검색합니다.
     *
     * @return 생성 시간 내림차순으로 정렬된 조건과 일치하는 {@link Study} 목록
     */
    @Query("""
    select s
    from Study s
    join fetch s.patientKey p
    where s.allowResearch = true
      and s.hiddenFlag = false
    order by s.createdAt desc
    """)
    List<Study> findResearchStudies();

    /**
     * 주어진 검사(study) 키 목록에 대한 Orthanc ID를 검색합니다.
     *
     * @param studyKeys 검사(study) 키 목록
     * @return 제공된 검사(study) 키에 해당하는 Orthanc ID 목록
     */
    @Query("""
            select s.orthancId from Study s
            where s.key in :studyKeys
    """)
    List<String> findOrthancUidFromKeys(@Param("studyKeys") List<Long> studyKeys);

    /**
     * 연구 허용 처리(익명화 요청) 시, Study의 Orthanc ID와 함께 소속 환자(patientKey)까지
     * 한 번에 가져옵니다. 환자별 날짜 시프트 오프셋을 계산하려면 patientKey가 필요해서 추가했다.
     * join fetch로 Patient를 같이 가져와서 N+1 쿼리(스터디마다 따로 환자 조회)를 피한다.
     *
     * @param studyKeys 검사(study) 키 목록
     * @return patientKey까지 함께 로딩된 {@link Study} 목록
     */
    @Query("""
            select s
            from Study s
            join fetch s.patientKey p
            where s.key in :studyKeys
    """)
    List<Study> findWithPatientByKeys(@Param("studyKeys") List<Long> studyKeys);
}
