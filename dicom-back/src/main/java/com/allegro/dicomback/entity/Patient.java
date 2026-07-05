package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @Column(name = "key")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long key;

    // 환자 성명
    @Column(name = "name", length = 64, nullable = false)
    private String name;

    // 환자 생년월일
    @Column(name = "birth", nullable = false)
    private LocalDate birth;

    // 환자 성별
    @Column(name = "sex", length = 2, nullable = false)
    private String sex;

    // 환자 최신 검사일자
    @Column(name = "recent_study")
    private LocalDateTime recentStudy;

    // 환자 Study 횟수
    @Builder.Default
    @Column(name = "study_count", nullable = false)
    private Integer studyCount = 0;

    // 환자 숨김 여부 (false: 정상, true: 삭제)
    @Builder.Default
    @Column(name = "hidden_flag", nullable = false)
    private Boolean hiddenFlag = false;

    // 담당의사 (외래키)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_key",  nullable = false)
    private User doctorKey;

    /// 환자 숨김 여부 설정(true: 숨김 설정, false: 숨김 해제)
    public void setHidden(boolean isHidden) {
        this.hiddenFlag = isHidden;
    }
}