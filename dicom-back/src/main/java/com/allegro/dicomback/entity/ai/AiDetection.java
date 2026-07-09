package com.allegro.dicomback.entity.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI가 탐지한 병변 박스 1개를 기록하는 엔티티.
@Entity
@Table(name = "ai_detections")
@Getter
@Setter
@NoArgsConstructor
public class AiDetection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DetectionKey")
    private Long detectionKey; // long(원시타입) 대신 Long(래퍼클래스) 권장

    // 이 박스가 어떤 AiResults(추론 작업)에서 나왔는지
    @Column(name = "ResultKey")
    private Long resultKey;

    // Orthanc가 부여한 instanceId(문자열)를 그대로 저장해 어떤 이미지인지 추적한다.
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