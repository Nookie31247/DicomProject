package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeriesRepository extends JpaRepository<Series, Long> {
    interface SeriesAndImagesCount {
        Long getStudyKey();
        Long getSeriesNum();
        Long getImagesNum();
    }

    @Query(
    """
    select
        s.studyKey.key as studyKey,
        count(s) as seriesNum,
        coalesce(sum(s.totalImagesCount), 0) as imagesNum
    from Series s
    where s.studyKey.key in :studyKeys
    group by s.studyKey.key
    """)
    List<SeriesAndImagesCount> getSeriesAndImagesCount(@Param("studyKeys") List<Long> studyKeys);

    @Query(
    """
    select se
    from Series se
    join se.studyKey st
    join st.patientKey p
    where p.doctorKey.key = :doctorKey
        and st.key = :studyKey
    """)
    List<Series> getSeries(
            @Param("doctorKey") Long doctorKey,
            @Param("studyKey") Long studyKey
    );

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
    // study에 속한 시리즈 목록 조회
    List<Series> findByStudyKey_KeyAndHiddenFlag(Long studyKey, Boolean hiddenFlag);

    // uid(Series Instance UID)로 기존 Series를 찾기 (있으면 재사용, 없으면 신규 생성)
    java.util.Optional<Series> findByUid(String uid);
}