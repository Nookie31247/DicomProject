package com.allegro.dicomback.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 컨트롤러 메서드에 붙이면 AuditLogAspect가 요청 성공 시 audit_logs에 자동으로 기록한다.
// 기획에 있던 "민감 정보 조회 시 실시간 행위 로그 영속화"를 AOP로 구현한 것.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogged {

    String action();      // 행위 유형: SEARCH, VIEW, HIDE, DOWNLOAD 등 (audit_logs.ActionType)

    String targetType();  // 대상 종류: PATIENT, STUDY, SERIES 등 (audit_logs.TargetType)

    // 대상 ID가 위치한 메서드 파라미터 순번(0부터). 목록 조회처럼 특정 대상이 없으면 -1(기본값)
    int targetArgIndex() default -1;
}