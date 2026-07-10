package com.allegro.backanonymization.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 연구를 나타내는 엔티티입니다.
 */
@Entity
@Table(name = "studies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Study {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long key;

    @Column(unique = true, name = "uid")
    private String uid;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "patient_birth")
    private LocalDate patientBirth;

    @Column(name = "patient_sex")
    private String patientSex;

    // 익명화 쪽에서 day shift처리하여 실제 검사 날짜를 가리는 가짜 검사일
    @Column(name = "study_date")
    private LocalDate studyDate;

    @Column(name = "description")
    private String description;

    @Column(name = "orthanc_id")
    private String orthancId;

}