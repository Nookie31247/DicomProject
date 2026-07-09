package com.allegro.dicomback.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 정보를 로깅하기 위한 어노테이션입니다.
 * 컨트롤러 메서드에 붙이면 AuditLogAspect가 요청 성공 시 audit_logs에 자동으로 기록한다.
 * 기획에 있던 "민감 정보 조회 시 실시간 행위 로그 영속화"를 AOP로 구현한 것.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogged {

    /**
     * 작업 유형: SEARCH, VIEW, HIDE, DOWNLOAD 등 (audit_logs.ActionType)
     *
     * @return 작업 문자열
     */
    String action();

    /**
     * 대상 유형: PATIENT, STUDY, SERIES 등 (audit_logs.TargetType)
     *
     * @return 대상 유형 문자열
     */
    String targetType();

    /**
     * 대상 ID를 포함하는 인수 인덱스입니다.
     * 대상 ID가 위치한 메서드 파라미터 순번(0부터). 목록 조회처럼 특정 대상이 없으면 -1(기본값)
     *
     * @return 인수 인덱스
     */
    int targetArgIndex() default -1;
}