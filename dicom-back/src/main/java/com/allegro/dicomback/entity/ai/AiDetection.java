package com.allegro.dicomback.entity.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ResultKey", nullable = false)
    private AiResults aiResult;

    @Column(name = "InstanceId", length = 64, nullable = false)
    private String instanceId;    // Orthanc instance UUID — "어느 이미지인지"의 새 식별자

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