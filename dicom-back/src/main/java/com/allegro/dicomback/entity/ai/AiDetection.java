package com.allegro.dicomback.entity.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI에 의해 감지된 단일 병변 상자를 기록하기 위한 엔티티입니다.
 */
@Entity
@Table(name = "ai_detections")
@Getter
@Setter
@NoArgsConstructor
public class AiDetection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DetectionKey")
    private Long detectionKey;

    // 이 박스가 어떤 AiResults(추론 작업)에서 나왔는지 기록
    @Column(name = "ResultKey")
    private Long resultKey;

    // Orthanc가 부여한 instanceId(문자열)를 그대로 저장해 어떤 이미지인지 추적
    @Column(name = "InstanceId", length = 128)
    private String instanceId;

    @Column(name = "Confidence")
    private Float confidence;

    @Column(name = "BoxX")
    private Integer boxX;

    @Column(name = "BoxY")
    private Integer boxY;

    @Column(name = "BoxWidth")
    private Integer boxWidth;

    @Column(name = "BoxHeight")
    private Integer boxHeight;

    @Column(name = "ClassName", length = 255)
    private String className;
}