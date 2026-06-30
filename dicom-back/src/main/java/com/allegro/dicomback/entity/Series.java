package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "series")
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
    private String seriesInstanceUid;

    @Column(name = "SeriesNum")
    private Integer seriesNum;

    @Column(name = "BodyPart", length = 64)
    private String bodyPart;

    // 소프트 삭제 여부 (0: 정상, 1: 삭제)
    @Builder.Default
    @Column(name = "DelFlag", nullable = false)
    private Integer delFlag = 0;

    // 소프트 삭제
    public void delete() {
        this.delFlag = 1;
    }

    //orthancSeriesId-> orthanc의 해시값을 받아서 Series 다운로드 용도으로 사용
    @Column(name = "orthancSeriesId")
    private String orthancSeriesId;
}