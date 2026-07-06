package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "studies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Study {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_key", nullable = false)
    private Patient patientKey;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "allow_research", nullable = false)
    private Boolean allowResearch = false;

    @Column(name = "orthanc_id")
    private String orthancId;

    @Builder.Default
    @Column(name = "hidden_flag", nullable = false)
    private Boolean hiddenFlag = false;

    // 스터디 숨김 여부 설정
    public void setHidden(boolean isHidden) {
        this.hiddenFlag = isHidden;
    }
}