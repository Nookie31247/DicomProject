package com.allegro.dicomback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * 보안 설정 클래스입니다.
 */
@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {

    // 다른 도메인으로의 요청을 받을 때 허용되는 목록들(,로 값 구분)
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * PasswordEncoder 빈을 제공합니다.
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 보안 필터 체인을 구성합니다.
     *
     * @param http 수정할 HttpSecurity
     * @param jwtTokenProvider JWT 토큰 제공자
     * @param redisTemplate Redis 템플릿
     * @return 구성된 SecurityFilterChain
     * @throws Exception 오류 발생 시
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider, RedisTemplate<String, String> redisTemplate) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/api/medical/users/login",
                                "/api/medical/users/signup",
                                "/api/medical/users/check-id",
                                "/api/medical/dicom/**",
                                "/api/medical/admin/**",
                                "/api/medical/ai/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
                    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                }));
        return http.build();
    }
}
