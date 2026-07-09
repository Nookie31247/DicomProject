package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.ai.AiDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * {@link AiDetection} 엔티티를 위한 레포지토리 인터페이스입니다.
 * 표준 CRUD 및 사용자 정의 쿼리 작업을 제공하기 위해 JpaRepository를 확장합니다.
 */
public interface AiDetectionRepository extends JpaRepository<AiDetection, Long> {

    /**
     * 특정 결과 키와 관련된 AI 감지 목록을 검색합니다.
     *
     * @param resultKey AI 분석의 결과 키
     * @return 결과 키와 일치하는 {@link AiDetection} 목록
     */
    List<AiDetection> findByResultKey(Long resultKey);

    /**
     * 특정 DICOM 인스턴스에 대한 AI 감지 목록을 검색합니다.
     *
     * @param instanceId 인스턴스의 ID
     * @return 인스턴스 ID와 일치하는 {@link AiDetection} 목록
     */
    List<AiDetection> findByInstanceId(String instanceId);
}