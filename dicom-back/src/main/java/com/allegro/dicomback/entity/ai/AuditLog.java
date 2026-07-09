package com.allegro.dicomback.entity.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 사용자 작업의 전체 내역(누가, 언제, 무엇을)을 기록하기 위한 로그입니다.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogKey")
    private Long logKey;

    @Column(name = "ActionType", length = 50)
    private String actionType;

    @Column(name = "UserKey")
    private Long userKey;

    @Column(name = "TargetUID", length = 128)
    private String targetUID;

    @Column(name = "TargetType", length = 30)
    private String targetType;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;
}