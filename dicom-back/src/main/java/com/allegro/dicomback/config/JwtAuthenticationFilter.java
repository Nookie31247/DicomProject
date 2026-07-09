package com.allegro.dicomback.config;

import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * JWT를 사용하여 요청을 인증하는 필터입니다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * JWT 인증을 위한 필터링을 수행합니다.
     *
     * @param request HTTP 서블릿 요청
     * @param response HTTP 서블릿 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 오류 발생 시
     * @throws IOException I/O 오류 발생 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPublicPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            writeError(response, ErrorCode.EMPTY_TOKEN);
            return;
        }

        try {
            jwtTokenProvider.validateToken(token);

            String isLogout = redisTemplate.opsForValue().get("jwt:blacklist:" + token);
            if (!ObjectUtils.isEmpty(isLogout)) {
                throw new BaseException(ErrorCode.INVALID_TOKEN);
            }

            String userId = jwtTokenProvider.getUserId(token);
            var auth = new UsernamePasswordAuthenticationToken(userId, null);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (BaseException e) {
            writeError(response, e.getErrorCode());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 주어진 요청 경로가 공개(public)인지 확인합니다.
     *
     * @param request HTTP 서블릿 요청
     * @return 경로가 공개된 경우 true, 그렇지 않으면 false
     */
    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/actuator/health")
                || path.startsWith("/api/medical/users/login")
                || path.startsWith("/api/medical/users/signup")
                || path.startsWith("/api/medical/users/check-id")
                || path.startsWith("/api/medical/dicom/")
                || path.startsWith("/api/medical/admin/")
                || path.startsWith("/api/medical/ai/");
    }

    /**
     * 요청 쿠키에서 JWT 토큰을 추출합니다.
     *
     * @param request HTTP 서블릿 요청
     * @return 추출된 토큰, 찾을 수 없으면 null
     */
    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 오류 응답을 작성합니다.
     *
     * @param response HTTP 서블릿 응답
     * @param errorCode 작성할 오류 코드
     * @throws IOException I/O 오류 발생 시
     */
    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
