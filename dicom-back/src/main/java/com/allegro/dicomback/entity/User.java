package com.allegro.dicomback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 시스템의 사용자를 나타내는 엔티티입니다.
 */
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

    /**
     * 사용자 ID에 접미사를 추가하고 상태를 false로 설정하여 사용자 계정을 비활성화합니다.
     */
    public void deactivate() {
        this.userId = this.userId + "$deactivate";
        this.userStatus = false;
        this.deletedAt = LocalDateTime.now();
    }
}