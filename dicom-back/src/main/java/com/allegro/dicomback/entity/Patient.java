package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;

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
    @Column(name = "id",  length = 64)
    private String id;

    // 환자 성명
    @Column(name = "name", length = 64)
    private String name;

    // 환자 생년월일
    @Column(name = "birth")
    private LocalDateTime birth;

    // 환자 성별
    @Column(name = "sex", length = 2)
    private String sex;

    // 환자 최신 검사일자
    @Column(name = "recent_study")
    private LocalDateTime recentStudy;

    // 환자 Study 횟수
    @Builder.Default
    @Column(name = "study_count", nullable = false)
    private Integer studyCount = 0;

    // 소프트 삭제 여부 (0: 정상, 1: 삭제)
    @Builder.Default
    @Column(name = "hidden_flag", nullable = false)
    private Integer hiddenFlag = 0;

    /// 환자 숨김 여부 설정(true: 숨김 설정, false: 숨김 해제)
    public void setHiddenFlag(boolean isHidden) {
        this.hiddenFlag = isHidden ? 1 : 0;
    }
}