package com.allegro.dicomback.config;

import com.allegro.dicomback.exception.*;
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

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret-key}")
    private String secretKeyStr;

    private SecretKey key;

    @PostConstruct
    protected void init() {
        // application.yml의 secret-key를 Base64로 디코딩하여 안전한 키 생성
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKeyStr));
    }

    // 토큰 생성 (userId와 role을 담음)
    public String createToken(String userId, Integer role) {
        Date now = new Date();

        // 토큰 유효기간: 24시간
        long EXPIRE_TIME = 1000L * 60 * 60 * 24;

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRE_TIME))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // 토큰 검증
    public void validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BaseException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
    }

    // 3. 토큰에서 Claims 전체 추출 (필터에서 유용하게 사용)
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 4. 토큰에서 유저 ID 추출
    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    // 5. 토큰에서 권한(Role) 추출
    public Integer getRole(String token) {
        return getClaims(token).get("role", Integer.class);
    }
}