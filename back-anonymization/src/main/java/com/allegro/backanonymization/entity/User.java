package com.allegro.backanonymization.entity;

import jakarta.persistence.*;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자를 나타내는 엔티티입니다.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 사용자 계정을 비활성화합니다.
     */
    public void deactivate() {
        this.userId = this.userId + "$deactivate";
        this.userStatus = false;
        this.deletedAt = LocalDateTime.now();
    }
}
