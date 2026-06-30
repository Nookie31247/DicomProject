package com.allegro.dicomback.service;

import com.allegro.dicomback.config.JwtTokenProvider;
import com.allegro.dicomback.dto.UserRequestDto.*;
import com.allegro.dicomback.dto.UserResponseDto;
import com.allegro.dicomback.entity.user.User;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public record LoginServiceRes(String token, String username) {}

    // 로그인
    public LoginServiceRes login(LoginRequest request) {
        // 유저 정보 조회
        User user = findActiveUser(request.userId());

        // 비밀번호 검증
        validatePassword(request.password(), user.getUserPassword());

        String token = jwtTokenProvider.createToken(user.getUserId(), user.getUserRole());
        String username = user.getUserName();

        return new LoginServiceRes(token, username);
    }

    // 회원가입
    @Transactional
    public void signup(SignupRequest request) {
        // 아이디 중복 체크
        if(checkIdDuplicate(request.userId())) {
            throw new BaseException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // userType은 UserRole(Integer)로 변환
        // 의료진 = 1, 연구원 = 2
        Integer role = switch (request.userRole()) {
            case "의료진" -> 1;
            case "연구원" -> 2;
            default -> throw new BaseException(ErrorCode.INVALID_USER_TYPE);
        };

        User user = User.builder()
                .userId(request.userId())
                .userPassword(passwordEncoder.encode(request.password()))
                .userName(request.name())
                .userRole(role)
                .build();

        userRepository.save(user);
    }

    // 비밀번호 수정
    @Transactional
    public void changePassword(ChangePasswordRequest request) {

        // 유저 정보 조회
        User user = findActiveUser(request.userId());

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
    }

    // 회원탈퇴
    @Transactional
    public void deleteUser(DeleteUserRequest request) {

        // 유저 정보 조회
        User user = findActiveUser(request.userId());

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
        String userRole = user.getUserRole() == 1 ? "의료진" : "연구원";
        LocalDate date = user.getCreatedAt().toLocalDate();

        return new UserResponseDto.UserInfoRes(userId, userRole, date);
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
