package com.allegro.dicomback.entity.anonymization;

import com.allegro.dicomback.entity.Study;
import com.allegro.dicomback.entity.User.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "anonymization_studies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnonymizationStudy {
    //가명화 로그 고유 번호
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AnonKey")
    private Long anonKey;

    //가명화된 환자 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StudyKey", nullable = false)
    private Study study;

    //가명화된 환자 ID
    @Column(name = "AnonPId", length = 64)
    private String anonPId;

    //가명화된 환자 생년월일
    @Column(name = "AnonPBirth")
    private LocalDateTime anonPBirth;

    //가명화된 환자 성명
    @Column(name = "AnonPName", length = 64)
    private String anonPName;

    //가명화된 StudyInstanceUID
    @Column(name = "AnonStudyUID", length = 128, unique = true)
    private String anonStudyUid;

    //날짜 가명화
    @Column(name = "DateShiftDays")
    private Integer dateShiftDays;

    //수행 사용자 키
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserKey", nullable = false)
    private User user;

    //가명화 수행 일시
    @Builder.Default
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    //가명화 끝나는 일시
    @Column(name = "ExportedAt")
    private LocalDateTime exportedAt;
}