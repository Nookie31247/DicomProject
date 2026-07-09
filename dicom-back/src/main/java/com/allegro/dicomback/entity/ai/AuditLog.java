package com.allegro.dicomback.entity.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

//누가 언제 뭘 했는지 행위 이력 전체를 남기는 로그
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