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
        s.study.key as studyKey,
        count(s) as seriesNum,
        coalesce(sum(s.totalInstanceCount), 0) as imagesNum
    from Series s
    where s.study.key in :studyKeys
    group by s.study.key
    """)
    List<SeriesAndImagesCount> getSeriesAndImagesCount(@Param("studyKeys") List<Long> studyKeys);
}