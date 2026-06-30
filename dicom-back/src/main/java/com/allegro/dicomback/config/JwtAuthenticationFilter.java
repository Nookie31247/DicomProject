package com.allegro.dicomback.config;

import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 로그인/회원가입 등 인증이 필요 없는 경로는 필터 건너뛰기
        String path = request.getRequestURI();
        if (path.startsWith("/api/users/login") || path.startsWith("/api/users/signup") || path.startsWith("/api/users/check-id")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 쿠키 배열에서 "token" 추출
        String token = null;
        if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(cookie -> "token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        // 토큰 존재 여부 확인 (Bearer 접두사 검사 제거)
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. 토큰 검증
            jwtTokenProvider.validateToken(token);

            //블랙리스트처리
            String isLogout = redisTemplate.opsForValue().get("jwt:blacklist:" + token);
            if (!ObjectUtils.isEmpty(isLogout)) {
                // 블랙리스트에 존재하면 예외 발생
                throw new BaseException(ErrorCode.INVALID_TOKEN);
            }

            // 3. 정보 추출
            String userId = jwtTokenProvider.getUserId(token);
            Integer role = jwtTokenProvider.getRole(token);

            // 4. SecurityContext에 인증 정보 저장
            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (BaseException e) {
            // 토큰이 만료되었거나 위조된 경우, 인증되지 않은 상태로 요청이 넘어가서
            // SecurityConfig의 설정에 따라 403 Forbidden 등으로 응답 처리됨
            
            // TODO 여기 토큰만료 커스텀 예외 추가하면 좋을듯
        }

        filterChain.doFilter(request, response);
    }
}