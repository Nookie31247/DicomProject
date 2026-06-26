package com.allegro.dicomback.entity.user;

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
    @Column(name = "UserKey")
    private Long userKey;

    @Column(name = "UserId", length = 50, nullable = false, unique = true)
    private String userId;

    @Builder.Default
    @Column(name = "UserRole", nullable = false, columnDefinition = "TINYINT")
    private Integer userRole = 1;

    @Column(name = "UserPassword", nullable = false)
    private String userPassword;

    @Column(name = "UserName", nullable = false)
    private String userName;

    @Builder.Default
    @Column(name = "UserStatus", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean userStatus = true;

    @Builder.Default
    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "DeletedAt")
    private LocalDateTime deletedAt;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "HospitalId")
    // private Hospital hospital;
}