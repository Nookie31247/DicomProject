package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.ai.AiResults;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * {@link AiResults} 엔티티를 위한 레포지토리 인터페이스입니다.
 * 표준 CRUD 및 사용자 정의 쿼리 작업을 제공하기 위해 JpaRepository를 확장합니다.
 */
public interface AiResultsRepository extends JpaRepository<AiResults, Long> {

    /**
     * 생성 시간 내림차순으로 정렬된 특정 시리즈에 대한 AI 추론 내역을 검색합니다.
     *
     * @param seriesKey 시리즈의 키
     * @return 시리즈 키와 일치하는 최신순으로 정렬된 {@link AiResults} 목록
     */
    List<AiResults> findBySeriesKeyOrderByCreatedAtDesc(Long seriesKey);
}