package com.allegro.backanonymization.config;

import com.allegro.backanonymization.exception.BaseException;
import com.allegro.backanonymization.exception.ErrorCode;
import com.allegro.backanonymization.exception.ErrorResponse;
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
import java.util.List;

/**
 * JWT 인증을 위한 필터입니다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 들어오는 요청을 필터링하고 JWT 토큰을 검증합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 오류가 발생한 경우
     * @throws IOException I/O 오류가 발생한 경우
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
            var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (BaseException e) {
            writeError(response, e.getErrorCode());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 요청 경로가 공개적인지 확인합니다.
     *
     * @param request HTTP 요청
     * @return 경로가 공개적인 경우 true, 그렇지 않으면 false
     */
    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/actuator/health")
                || path.startsWith("/api/research/users/login")
                || path.startsWith("/api/research/users/signup")
                || path.startsWith("/api/research/users/check-id")
                || path.startsWith("/api/research/dicom/");
    }

    /**
     * 요청 쿠키에서 JWT 토큰을 추출합니다.
     *
     * @param request HTTP 요청
     * @return 추출된 토큰, 찾을 수 없는 경우 null
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
     * 지정된 오류 코드로 오류 응답을 작성합니다.
     *
     * @param response HTTP 응답
     * @param errorCode 오류 코드
     * @throws IOException I/O 오류가 발생한 경우
     */
    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
