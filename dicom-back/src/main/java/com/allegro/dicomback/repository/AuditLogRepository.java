package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.ai.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * {@link AuditLog} 엔티티를 위한 레포지토리 인터페이스입니다.
 * 표준 CRUD 및 사용자 정의 쿼리 작업을 제공하기 위해 JpaRepository를 확장합니다.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 특정 사용자에 대한 감사 로그를 생성 시간 내림차순으로 정렬하여 검색합니다.
     *
     * @param userKey 사용자 키
     * @return 최신순으로 정렬된 사용자 키와 일치하는 {@link AuditLog} 목록
     */
    List<AuditLog> findByUserKeyOrderByCreatedAtDesc(Long userKey);

    /**
     * 특정 DICOM 객체(환자 검사, 이미지 등)에 대한 액세스를 추적하는 감사 로그를 검색합니다.
     *
     * @param targetUID DICOM 객체의 대상 UID
     * @return 대상 UID와 일치하는 {@link AuditLog} 목록
     */
    List<AuditLog> findByTargetUID(String targetUID);

    /**
     * 특정 작업 유형(예: DELETE, AI_INFER)으로 필터링된 감사 로그를 검색합니다.
     *
     * @param actionType 필터링할 작업 유형
     * @return 작업 유형과 일치하는 {@link AuditLog} 목록
     */
    List<AuditLog> findByActionType(String actionType);
}