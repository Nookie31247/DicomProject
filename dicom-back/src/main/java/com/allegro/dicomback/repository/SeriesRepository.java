package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
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
}