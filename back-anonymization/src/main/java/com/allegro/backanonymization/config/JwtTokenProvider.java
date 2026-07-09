package com.allegro.backanonymization.config;

import com.allegro.backanonymization.exception.BaseException;
import com.allegro.backanonymization.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰을 생성하고 검증하기 위한 프로바이더입니다.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret-key}")
    private String secretKeyStr;

    private SecretKey key;

    /**
     * 비밀 키를 초기화합니다.
     */
    @PostConstruct
    protected void init() {
        // application.yml의 secret-key를 Base64로 디코딩하여 안전한 키 생성
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyStr));
    }

    /**
     * 사용자를 위한 JWT 토큰을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param userKey 사용자 키
     * @return 생성된 JWT 토큰
     */
    public String createToken(String userId, Long userKey) {
        Date now = new Date();

        // 토큰 유효기간: 24시간
        long EXPIRE_TIME = 1000L * 60 * 60 * 24;

        return Jwts.builder()
                .subject(userId)
                .claim("userKey", userKey)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRE_TIME))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 지정된 JWT 토큰을 검증합니다.
     *
     * @param token 검증할 JWT 토큰
     */
    public void validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BaseException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * JWT 토큰에서 모든 클레임을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 클레임
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * JWT 토큰에서 사용자 ID를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * JWT 토큰에서 사용자 키를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 키
     */
    public Long getUserKey(String token) {
        return getClaims(token).get("userKey", Long.class);
    }
}
