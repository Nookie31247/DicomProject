package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "studies",
        indexes = {
                @Index(name = "idx_study_uid", columnList = "StudyInstanceUID", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "key")
    private Long key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "study_instance_uid", unique = true, length = 128)
    private String studyInstanceUid;

    @Column(name = "study_date_time")
    private LocalDateTime studyDateTime;

    @Column(name = "description")
    private String description;

//    // --- 통계 정보 추가 ---
//    @Column(name = "TotalSeriesCount")
//    private Integer totalSeriesCount; // 전체 시리즈 개수
//
//    @Column(name = "TotalInstanceCount")
//    private Integer totalInstanceCount; // 해당 시리즈 내의 이미지 개수

    // 연구 데이터 사용 허가 여부(0: 금지, 1: 허용)
    @Builder.Default
    @Column(name = "AllowedResearch", nullable = false)
    private Byte allowedResearch = 0;

    @Column(name = "orthanc_id")
    private String orthancId;

    @Builder.Default
    @Column(name = "hidden_flag", nullable = false)
    private Integer hiddenFlag = 0;

    // 소프트 삭제1
    public void delete() {
        this.hiddenFlag = 1;
    }
}