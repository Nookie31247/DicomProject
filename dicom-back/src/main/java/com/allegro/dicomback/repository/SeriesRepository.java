package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeriesRepository extends JpaRepository<Series, Long> {

    // study에 속한 시리즈 목록 조회
    List<Series> findByStudy_StudyKeyAndDelFlag(Long studyKey, Integer delFlag);

    // 고유 시리즈 UID로 단건 조회
    Series findBySeriesInstanceUID(String seriesInstanceUID);

    // AI 판독 전용 시리즈(SeriesNum이 9000번대 이상) 조회 시 활용
    List<Series> findByStudy_StudyKeyAndSeriesNumGreaterThanEqual(Long studyKey, Integer seriesNum);
}