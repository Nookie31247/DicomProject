package com.allegro.dicomback.log;

import com.allegro.dicomback.config.JwtTokenProvider;
import com.allegro.dicomback.entity.ai.AuditLog;
import com.allegro.dicomback.repository.AuditLogRepository;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 감사 정보를 로깅하기 위한 Aspect입니다.
 * @AuditLogged가 붙은 메서드가 "정상적으로" 끝났을 때만 로그를 남긴다.
 * (AfterReturning은 예외가 나서 실패한 요청에는 실행되지 않는다 - 실패 요청까지 남기고 싶으면 별도 처리 필요)
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 성공적인 실행 후 감사 정보를 로깅합니다.
     *
     * @param joinPoint 조인 포인트
     * @param auditLogged 감사 로그 어노테이션
     */
    @AfterReturning("@annotation(auditLogged)")
    public void logAfterSuccess(JoinPoint joinPoint, AuditLogged auditLogged) {
        try {
            Object[] args = joinPoint.getArgs();
            String targetUid = "-";
            int idx = auditLogged.targetArgIndex();
            if (idx >= 0 && idx < args.length && args[idx] != null) {
                targetUid = String.valueOf(args[idx]);
            }

            AuditLog entry = new AuditLog();
            entry.setUserKey(resolveCurrentUserKey());
            entry.setActionType(auditLogged.action());
            entry.setTargetType(auditLogged.targetType());
            entry.setTargetUID(targetUid);
            entry.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // 로그 적재 실패가 실제 API 응답에 영향을 주면 안 되므로 예외를 여기서 흡수한다.
            log.warn("감사 로그 적재 실패: {}", e.getMessage());
        }
    }

    /**
     * 요청 토큰에서 현재 사용자 키를 확인합니다.
     * 현재 요청의 쿠키에서 JWT를 꺼내 사용자 key를 알아낸다. 없거나 유효하지 않으면 null(익명 기록).
     *
     * @return 사용자 키 또는 null
     */
    private Long resolveCurrentUserKey() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;

        Cookie[] cookies = attrs.getRequest().getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                try {
                    return jwtTokenProvider.getUserKey(cookie.getValue());
                } catch (Exception e) {
                    return null; // 만료/위조 토큰이면 익명으로 기록
                }
            }
        }
        return null;
    }
}