package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * {@link Series} 엔티티를 위한 레포지토리 인터페이스입니다.
 * 표준 CRUD 및 사용자 정의 쿼리 작업을 제공하기 위해 JpaRepository를 확장합니다.
 */
public interface SeriesRepository extends JpaRepository<Series, Long> {

    /**
     * 검사(study)당 시리즈 수 및 총 이미지 수를 집계하기 위한 프로젝션 인터페이스입니다.
     */
    interface SeriesAndImagesCount {
        Long getStudyKey();
        Long getSeriesNum();
        Long getImagesNum();
    }

    /**
     * 주어진 검사(study) 키 목록에 대해 시리즈 및 이미지의 집계된 수를 검색합니다.
     *
     * +PR는 표기되지 않게 배제했기엔 카운트가 되선 안된다.
     *
     * @param studyKeys 집계할 검사(study) 키 목록
     * @return 집계된 데이터를 포함하는 {@link SeriesAndImagesCount} 프로젝션 목록
     */
    @Query(
    """
    select
        s.studyKey.key as studyKey,
        count(s) as seriesNum,
        coalesce(sum(s.totalImagesCount), 0) as imagesNum
    from Series s
    where s.studyKey.key in :studyKeys
      and (s.modality is null or upper(s.modality) not in ('PR'))
    group by s.studyKey.key
    """)
    List<SeriesAndImagesCount> getSeriesAndImagesCount(@Param("studyKeys") List<Long> studyKeys);

    /**
     * 특정 의사 및 검사(study)에 대한 모든 시리즈를 검색합니다.
     *
     * @param doctorKey 의사 키
     * @param studyKey 검사(study) 키
     * @return 해당 검사(study)에 속하고 의사가 접근할 수 있는 {@link Series} 목록
     */
    @Query(
    """
    select se
    from Series se
    join se.studyKey st
    join st.patientKey p
    where p.doctorKey.key = :doctorKey
        and st.key = :studyKey
    order by se.seriesNum asc
    """)
    List<Series> getSeries(
            @Param("doctorKey") Long doctorKey,
            @Param("studyKey") Long studyKey
    );

    /**
     * 특정 의사의 환자에 속하는 여러 시리즈에 대한 숨김 플래그를 업데이트합니다.
     *
     * @param doctorKey 의사 키
     * @param seriesKeys 업데이트할 시리즈 키 목록
     * @param isHidden 새로운 숨김 플래그 상태
     * @return 업데이트된 레코드 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
    """
        update Series se
        set se.hiddenFlag = :isHidden
        where se.key in :seriesKeys
        and exists (
            select 1
            from Study st
            where st = se.studyKey
            and st.patientKey.doctorKey.key = :doctorKey
        )
    """
    )
    int changeHiddenFlag(
            @Param("doctorKey") Long doctorKey,
            @Param("seriesKeys") List<Long> seriesKeys,
            @Param("isHidden") boolean isHidden
    );

    /**
     * 숨김 플래그로 필터링된 주어진 검사(study)에 대한 시리즈 목록을 검색합니다.
     *
     * @param studyKey 검사(study) 키
     * @param hiddenFlag 필터링할 숨김 플래그 상태
     * @return 조건과 일치하는 {@link Series} 목록
     */
    List<Series> findByStudyKey_KeyAndHiddenFlag(Long studyKey, Boolean hiddenFlag);

    /**
     * Series Instance UID로 시리즈를 찾습니다. 기존 시리즈를 재사용하는 데 사용할 수 있습니다.
     *
     * @param uid Series Instance UID
     * @return 찾은 경우 {@link Series}를 포함하는 {@link java.util.Optional}, 그렇지 않으면 비어 있음
     */
    java.util.Optional<Series> findByUid(String uid);
}