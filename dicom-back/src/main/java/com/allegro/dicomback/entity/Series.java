package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DICOM 시리즈를 나타내는 엔티티입니다.
 */
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

    @Column(name = "uid", unique = true)
    private String uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_key", nullable = false)
    private Study studyKey;

    @Column(name = "series_num")
    private Integer seriesNum;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "body_part", length = 64)
    private String bodyPart;

    @Column(name = "series_description", length = 255)
    private String seriesDescription;

    @Column(name = "modality", length = 16)
    private String modality;

    // orthancSeriesId -> orthanc의 해시값을 받아서 Series 다운로드 용도로 사용
    @Column(name = "orthanc_id")
    private String orthancId;

    @Column(name = "total_images_conut")
    private Integer totalImagesCount = 0;

    // 소프트 삭제 여부 (0: 정상, 1: 삭제)
    @Builder.Default
    @Column(name = "hidden_flag", nullable = false)
    private Boolean hiddenFlag = false;

    /**
     * 숨김 플래그를 설정하여 시리즈를 소프트 삭제하거나 복원합니다.
     *
     * @param isHidden 숨기려면 true, 복원하려면 false
     */
    public void setHidden(boolean isHidden) {
        this.hiddenFlag = isHidden;
    }
}