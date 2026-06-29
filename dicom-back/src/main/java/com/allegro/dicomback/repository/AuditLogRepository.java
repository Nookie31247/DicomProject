package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.ai.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    //로그를 최신순(내림차순)으로 조회
    List<AuditLog> findByUserKeyOrderByCreatedAtDesc(Long userKey);

    // 특정 DICOM 객체(TargetUID - 환자 검사, 이미지 등)에 대한 접근 기록 추적
    List<AuditLog> findByTargetUID(String targetUID);

    //DELETE, AI_INFER에 해당하는 로그만 조회
    List<AuditLog> findByActionType(String actionType);
}