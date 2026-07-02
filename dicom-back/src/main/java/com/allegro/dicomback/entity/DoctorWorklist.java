package com.allegro.dicomback.entity;

import com.allegro.dicomback.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doctor_worklist")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorWorklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserKey", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PId", nullable = false)
    private Patient patient;

    // 검사 할당 전에는 null 가능, 할당 시 Study 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StudyKey")
    private Study study;
}