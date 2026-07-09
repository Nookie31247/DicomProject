package com.allegro.backanonymization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 시리즈를 나타내는 엔티티입니다.
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

    @Column(name = "modality", length = 16)
    private String modality;

    @Column(name = "orthanc_id")
    private String orthancId;

    @Column(name = "total_images_conut")
    private Integer totalImagesCount = 0;

    @Builder.Default
    @Column(name = "hidden_flag", nullable = false)
    private Boolean hiddenFlag = false;

    /**
     * 시리즈의 숨김 플래그를 설정합니다.
     *
     * @param isHidden 숨김 여부 (true면 숨김, 그렇지 않으면 false)
     */
    public void setHidden(boolean isHidden) {
        this.hiddenFlag = isHidden;
    }
}