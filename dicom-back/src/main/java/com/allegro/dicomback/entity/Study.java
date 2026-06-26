package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.autoconfigure.security.SecurityProperties;

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
    @Column(name = "StudyKey")
    private Long studyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Doctor", referencedColumnName = "UserName", nullable = false)
    private SecurityProperties.User doctor;

    @Column(name = "Modality", length = 16)
    private String modality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PId", nullable = false)
    private Patient patient;

    @Column(name = "StudyInstanceUID", unique = true, length = 128)
    private String studyInstanceUid;

    @Column(name = "StudyDateTime")
    private LocalDateTime studyDateTime;

    @Builder.Default
    @Column(name = "AllowedResearch", nullable = false)
    private Byte allowedResearch = 0;

    @Builder.Default
    @Column(name = "AnonFlag", nullable = false)
    private Byte anonFlag = 0;

    // 소프트 삭제 여부 (0: 정상, 1: 삭제)
    @Builder.Default
    @Column(name = "DelFlag", nullable = false)
    private Integer delFlag = 0;

    // 소프트 삭제
    public void delete() {
        this.delFlag = 1;
    }
}