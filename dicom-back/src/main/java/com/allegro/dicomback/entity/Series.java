package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    @Column(name = "id")
    private Long key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_key", nullable = false)
    private Study studyKey;

    @Column(name = "series_num")
    private Integer seriesNum;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "body_part", length = 64)
    private String bodyPart;

    @Column(name = "modality", length = 16)
    private String modality;

    //orthancSeriesId-> orthanc의 해시값을 받아서 Series 다운로드 용도으로 사용
    @Column(name = "orthanc_id")
    private String orthancId;

    @Column(name = "total_images_conut")
    private Integer totalImagesCount; // 해당 시리즈 내의 이미지 개수

    // 소프트 삭제 여부 (0: 정상, 1: 삭제)
    @Builder.Default
    @Column(name = "hidden_flag", nullable = false)
    private Boolean hiddenFlag = false;

    // 시리즈 숨김 여부 설정
    public void setHidden(boolean isHidden) {
        this.hiddenFlag = isHidden;
    }
}