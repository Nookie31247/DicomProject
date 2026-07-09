package com.allegro.dicomback.service;

import com.allegro.dicomback.config.JwtTokenProvider;
import com.allegro.dicomback.dto.UserRequestDto.*;
import com.allegro.dicomback.dto.UserResponseDto;
import com.allegro.dicomback.entity.User;
import com.allegro.dicomback.entity.ai.AuditLog;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.AuditLogRepository;
import com.allegro.dicomback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 관련 작업을 관리하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogRepository auditLogRepository;

    public record LoginServiceRes(String token, String username) {}

    /**
     * 사용자 로그인을 수행합니다.
     *
     * @param request 로그인 요청
     * @return 로그인 응답
     */
    @Transactional
    public LoginServiceRes login(LoginRequest request) {
        // 유저 정보 조회
        User user = findActiveUser(request.userId());

        // 비밀번호 검증
        validatePassword(request.password(), user.getUserPassword());

        String token = jwtTokenProvider.createToken(user.getUserId(), user.getKey());
        String username = user.getUserName();

        // 로그인 성공 시점에 감사 로그 기록
        AuditLog log = new AuditLog();
        log.setUserKey(user.getKey());
        log.setActionType("Login");
        log.setTargetType("User");
        log.setTargetUID(String.valueOf(user.getKey()));
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);

        return new LoginServiceRes(token, username);
    }

    /**
     * 사용자 회원가입을 수행합니다.
     *
     * @param request 회원가입 요청
     */
    @Transactional
    public void signup(SignupRequest request) {
        // 아이디 중복 체크
        if(checkIdDuplicate(request.userId())) {
            throw new BaseException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.builder()
                .userId(request.userId())
                .userPassword(passwordEncoder.encode(request.password()))
                .userName(request.name())
                .build();

        userRepository.save(user);
    }

    /**
     * 사용자 비밀번호를 변경합니다.
     *
     * @param token 사용자 토큰
     * @param request 비밀번호 변경 요청
     */
    @Transactional
    public void changePassword(String token, ChangePasswordRequest request) {
        // 유저 정보 조회
        User user = findActiveUser(jwtTokenProvider.getUserId(token));

        // 기존 비밀번호 검증
        validatePassword(request.currentPassword(), user.getUserPassword());

        // 새 비밀번호 암호화 후 업데이트
        user.setUserPassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 토큰을 블랙리스트에 추가하여 사용자 로그아웃을 수행합니다.
     *
     * @param token 사용자 토큰
     */
    @Transactional
    public void logout(String token) {
        String redisKey= "jwt:blacklist:" + token;
        redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);

        // 로그아웃도 감사 로그에 기록 (토큰이 블랙리스트에 올라가기 전에 userKey를 미리 꺼내둔다)
        try {
            Long userKey = jwtTokenProvider.getUserKey(token);
            AuditLog log = new AuditLog();
            log.setUserKey(userKey);
            log.setActionType("Login");
            log.setTargetType("User");
            log.setTargetUID(String.valueOf(userKey));
            log.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) {
            log.warn("로그아웃 감사 로그 기록 실패: {}", e.getMessage());
        }
    }

    /**
     * 사용자 계정을 비활성화합니다.
     *
     * @param token 사용자 토큰
     * @param request 사용자 삭제 요청
     */
    @Transactional
    public void deleteUser(String token, DeleteUserRequest request) {

        // 유저 정보 조회
        User user = findActiveUser(jwtTokenProvider.getUserId(token));

        // 비밀번호 검증
        validatePassword(request.password(), user.getUserPassword());

        // 소프트 삭제 처리
        user.deactivate();
    }

    /**
     * 사용자 ID가 중복되는지 확인합니다.
     *
     * @param userId 확인할 사용자 ID
     * @return 중복되면 true, 그렇지 않으면 false
     */
    public boolean checkIdDuplicate(String userId) {
        return userRepository.existsByUserId(userId);
    }

    /**
     * 사용자 정보를 검색합니다.
     *
     * @param token 사용자 토큰
     * @return 사용자 정보 응답
     */
    public UserResponseDto.UserInfoRes getUserInfo(String token) {
        String userId = jwtTokenProvider.getUserId(token);
        User user = findActiveUser(userId);
        String username = user.getUserName();
        LocalDate date = user.getCreatedAt().toLocalDate();

        return new UserResponseDto.UserInfoRes(userId, username, date);
    }

    // --- [공통] 유저 조회 (Private) ---
    private User findActiveUser(String userId) {
        return userRepository.findByUserIdAndUserStatusTrue(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    // --- [공통] 비밀번호 검증 (Private) ---
    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BaseException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
