package com.allegro.backanonymization.repository;

import com.allegro.backanonymization.entity.Series;
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

    boolean existsByUid(String uid);

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

    List<Series> findSeriesByStudyKey_Key(Long studyKey);
}