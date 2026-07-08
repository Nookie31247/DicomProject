package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.ai.AiResults;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiResultsRepository extends JpaRepository<AiResults, Long> {

    // 특정 시리즈에 대해 지금까지 수행된 AI 추론 이력을 최신순으로 조회
    List<AiResults> findBySeriesKeyOrderByCreatedAtDesc(Long seriesKey);
}