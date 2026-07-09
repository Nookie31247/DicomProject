package com.allegro.backanonymization.repository;

import com.allegro.backanonymization.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Series 엔티티를 위한 레포지토리 인터페이스입니다.
 */
public interface SeriesRepository extends JpaRepository<Series, Long> {
    interface SeriesAndImagesCount {
        Long getStudyKey();
        Long getSeriesNum();
        Long getImagesNum();
    }

    /**
     * UID로 시리즈가 존재하는지 확인합니다.
     *
     * @param uid UID
     * @return 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByUid(String uid);

    /**
     * 연구 키 목록에 대한 시리즈 및 이미지 수를 가져옵니다.
     *
     * @param studyKeys 연구 키 목록
     * @return SeriesAndImagesCount 목록
     */
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

    /**
     * 연구 키로 시리즈를 찾습니다.
     *
     * @param studyKey 연구 키
     * @return 시리즈 목록
     */
    List<Series> findSeriesByStudyKey_Key(Long studyKey);
}