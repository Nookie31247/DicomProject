package com.allegro.dicomback.service;

import com.allegro.dicomback.config.JwtTokenProvider;
import com.allegro.dicomback.dto.UserRequestDto.*;
import com.allegro.dicomback.dto.UserResponseDto;
import com.allegro.dicomback.entity.User;
import com.allegro.dicomback.entity.UserType;
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

    // 로그인
    @Transactional
    public LoginServiceRes login(LoginRequest request) {
        // 유저 정보 조회
        User user = findActiveUser(request.userId());

        // 비밀번호 검증
        validatePassword(request.password(), user.getUserPassword());

        String token = jwtTokenProvider.createToken(user.getUserId(), user.getUserType().getTypeString(), user.getKey());
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

    // 회원가입
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
                .userType(UserType.fromTypeString(request.userType()))
                .build();

        userRepository.save(user);
    }

    // 비밀번호 수정
    @Transactional
    public void changePassword(String token, ChangePasswordRequest request) {
        // 유저 정보 조회
        User user = findActiveUser(jwtTokenProvider.getUserId(token));

        // 기존 비밀번호 검증
        validatePassword(request.currentPassword(), user.getUserPassword());

        // 새 비밀번호 암호화 후 업데이트
        user.setUserPassword(passwordEncoder.encode(request.newPassword()));
    }

    // 로그아웃 (블랙리스트 형식으로)
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

    // 회원탈퇴
    @Transactional
    public void deleteUser(String token, DeleteUserRequest request) {

        // 유저 정보 조회
        User user = findActiveUser(jwtTokenProvider.getUserId(token));

        // 비밀번호 검증
        validatePassword(request.password(), user.getUserPassword());

        // 소프트 삭제 처리
        user.deactivate();
    }

    // 아이디 중복 확인
    public boolean checkIdDuplicate(String userId) {
        return userRepository.existsByUserId(userId);
    }

    // 유저 정보 조회
    public UserResponseDto.UserInfoRes getUserInfo(String token) {
        String userId = jwtTokenProvider.getUserId(token);
        User user = findActiveUser(userId);
        String username = user.getUserName();
        String userType = user.getUserType().getTypeString();
        LocalDate date = user.getCreatedAt().toLocalDate();

        return new UserResponseDto.UserInfoRes(userId, username, userType, date);
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
