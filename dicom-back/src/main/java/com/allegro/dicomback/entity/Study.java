package com.allegro.dicomback.entity;

import com.allegro.dicomback.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    @Column(name = "StudyKey")
    private Long studyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Doctor")
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PId", nullable = false)
    private Patient patient;

    @Column(name = "StudyInstanceUID", unique = true, length = 128)
    private String studyInstanceUid;

    @Column(name = "StudyDateTime")
    private LocalDateTime studyDateTime;

    @Column(name = "Description")
    private String description;

    @Column(name = "AccessionNumber")
    private String accessionNumber;

    // --- 통계 정보 추가 ---
    @Column(name = "TotalSeriesCount")
    private Integer totalSeriesCount; // 전체 시리즈 개수

    @Column(name = "TotalInstanceCount")
    private Integer totalInstanceCount; // 해당 시리즈 내의 이미지 개수

    // 연구 데이터 사용 허가 여부(0: 금지, 1: 허용)
    @Builder.Default
    @Column(name = "AllowedResearch", nullable = false)
    private Byte allowedResearch = 0;

    @Column(name = "OrthancStudyId")
    private String orthancStudyId;

    // 익명화 처리 완료 여부 혹은 익명화 가능 상태 (0: 미처리, 1: 처리됨/가능)
    @Builder.Default
    @Column(name = "AnonFlag", nullable = false)
    private Byte anonFlag = 0;

    @Builder.Default
    @Column(name = "DelFlag", nullable = false)
    private Integer delFlag = 0;

    // 소프트 삭제1
    public void delete() {
        this.delFlag = 1;
    }
}