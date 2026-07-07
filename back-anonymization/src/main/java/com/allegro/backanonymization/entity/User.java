package com.allegro.backanonymization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long key;

    @Column(name = "user_id", length = 50, nullable = false, unique = true)
    private String userId;

    @Column(name = "user_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(name = "user_password", nullable = false)
    private String userPassword;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Builder.Default
    @Column(name = "user_status", nullable = false)
    private Boolean userStatus = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void deactivate() {
        this.userId = this.userId + "$deactivate";
        this.userStatus = false;
        this.deletedAt = LocalDateTime.now(); //탈퇴 시간 기록
    }
}