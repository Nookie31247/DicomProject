package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "series", indexes = {
        @Index(name = "idx_series_uid", columnList = "SeriesInstanceUID", unique = true),
        @Index(name = "idx_study_key", columnList = "StudyKey") // FK도 인덱스 필수
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SeriesKey")
    private Long seriesKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StudyKey", nullable = false)
    private Study study;

    @Column(name = "SeriesInstanceUID", unique = true, length = 128)
    private String seriesInstanceUID;

    @Column(name = "SeriesNum")
    private Integer seriesNum;

    @Column(name = "BodyPart", length = 64)
    private String bodyPart;

    @Column(name = "Modality", length = 16)
    private String modality;

    @Column(name = "OrthancSeriesId")
    private String orthancSeriesId;

    // --- 통계 정보 추가 ---
    @Column(name = "TotalInstanceCount")
    private Integer totalInstanceCount; // 해당 시리즈 내의 이미지 개수

    @Builder.Default
    @Column(name = "DelFlag", nullable = false)
    private Integer delFlag = 0;

    // 소프트 삭제
    public void delete() {
        this.delFlag = 1;
    }
}