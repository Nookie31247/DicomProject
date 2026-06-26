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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PId")
    private Long pId;

    // 환자 성명
    @Column(name = "PName", length = 64)
    private String pName;

    // 환자 생년월일
    @Column(name = "PBirth")
    private LocalDateTime pBirth;

    // 환자 성별
    @Column(name = "PSex", length = 2)
    private String pSex;

    // 환자 최신 검사일자
    @Column(name = "PTime")
    private LocalDateTime pTime;

    // 환자 Study 횟수
    @Builder.Default
    @Column(name = "PStudyCount", nullable = false)
    private Integer pStudyCount = 0;

    // 소프트 삭제 여부 (0: 정상, 1: 삭제)
    @Builder.Default
    @Column(name = "DelFlag", nullable = false)
    private Integer delFlag = 0;

    // 소프트 삭제
    public void delete() {
        this.delFlag = 1;
    }
}