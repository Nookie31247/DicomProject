package com.allegro.backanonymization.entity.ai;

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
    private Long detectionKey; // long(원시타입) 대신 Long(래퍼클래스) 권장

    @Column(name = "ResultKey")
    private Long resultKey;

    @Column(name = "ImageKey")
    private Long imageKey;

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