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

    @Column(name = "series_instance_uid", unique = true, length = 128)
    private String seriesInstanceUid;

    @Column(name = "series_num")
    private Integer seriesNum;

    @Column(name = "body_part", length = 64)
    private String bodyPart;

    @Column(name = "modality", length = 16)
    private String modality;

    //orthancSeriesId-> orthanc의 해시값을 받아서 Series 다운로드 용도으로 사용
    @Column(name = "orthanc_id")
    private String orthancId;

    @Column(name = "total_instance_conut")
    private Integer totalInstanceCount; // 해당 시리즈 내의 이미지 개수

    // 소프트 삭제 여부 (0: 정상, 1: 삭제)
    @Builder.Default
    @Column(name = "hidden_flag", nullable = false)
    private Integer hiddenFlag = 0;

    // 소프트 삭제
    public void delete() {
        this.hiddenFlag = 1;
    }
}