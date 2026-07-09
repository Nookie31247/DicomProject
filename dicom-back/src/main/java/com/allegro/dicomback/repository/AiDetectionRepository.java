package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.ai.AiDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiDetectionRepository extends JpaRepository<AiDetection, Long> {

    // 특정 AI 분석 결과(ResultKey)에 속한 박스 목록 조회
    List<AiDetection> findByResultKey(Long resultKey);

    // 특정 인스턴스에서 탐지된 병변만 필터링
    List<AiDetection> findByInstanceId(String  instanceId);
}