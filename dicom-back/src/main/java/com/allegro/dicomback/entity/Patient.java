package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 환자를 나타내는 엔티티입니다.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long key;

    @Column(name = "name", length = 64, nullable = false)
    private String name;

    @Column(name = "birth", nullable = false)
    private LocalDate birth;

    @Column(name = "sex", length = 2, nullable = false)
    private String sex;

    @Column(name = "recent_study")
    private LocalDateTime recentStudy;

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

    /**
     * 숨김 플래그를 설정하여 환자를 소프트 삭제하거나 복원합니다.
     *
     * @param isHidden 숨기려면 true, 복원하려면 false
     */
    public void setHidden(boolean isHidden) {
        this.hiddenFlag = isHidden;
    }
}