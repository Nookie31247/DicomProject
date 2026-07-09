package com.allegro.backanonymization.service;

import com.allegro.backanonymization.config.JwtTokenProvider;
import com.allegro.backanonymization.dto.UserRequestDto.*;
import com.allegro.backanonymization.dto.UserResponseDto;
import com.allegro.backanonymization.entity.User;
import com.allegro.backanonymization.exception.BaseException;
import com.allegro.backanonymization.exception.ErrorCode;
import com.allegro.backanonymization.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.allegro.backanonymization.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 작업을 위한 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public record LoginServiceRes(String token, String username) {}

    /**
     * 사용자를 인증하고 토큰을 반환합니다.
     *
     * @param request 로그인 요청
     * @return 토큰을 포함하는 로그인 응답
     */
    public LoginServiceRes login(LoginRequest request) {
        // 유저 정보 조회
        User user = findActiveUser(request.userId());

        // 비밀번호 검증
        validatePassword(request.password(), user.getUserPassword());

        String token = jwtTokenProvider.createToken(user.getUserId(), user.getKey());
        String username = user.getUserName();

        return new LoginServiceRes(token, username);
    }

    /**
     * 새로운 사용자를 등록합니다.
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
     * 사용자의 비밀번호를 변경합니다.
     *
     * @param token JWT 토큰
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
     * 토큰을 블랙리스트에 추가하여 사용자를 로그아웃합니다.
     *
     * @param token JWT 토큰
     */
    @Transactional
    public void logout(String token) {
        String redisKey= "jwt:blacklist:" + token;
        redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
    }

    /**
     * 사용자 계정을 비활성화합니다.
     *
     * @param token JWT 토큰
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
     * @param userId 사용자 ID
     * @return 중복되면 true, 그렇지 않으면 false
     */
    public boolean checkIdDuplicate(String userId) {
        return userRepository.existsByUserId(userId);
    }

    /**
     * 사용자 정보를 가져옵니다.
     *
     * @param token JWT 토큰
     * @return 사용자 정보 응답
     */
    public UserResponseDto.UserInfoRes getUserInfo(String token) {
        String userId = jwtTokenProvider.getUserId(token);
        User user = findActiveUser(userId);
        String username = user.getUserName();
        LocalDate date = user.getCreatedAt().toLocalDate();

        return new UserResponseDto.UserInfoRes(userId, username, date);
    }

    /**
     * ID로 활성 사용자를 찾습니다.
     *
     * @param userId 사용자 ID
     * @return 사용자
     */
    private User findActiveUser(String userId) {
        return userRepository.findByUserIdAndUserStatusTrue(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 인코딩된 비밀번호와 원본 비밀번호를 검증합니다.
     *
     * @param rawPassword 원본 비밀번호
     * @param encodedPassword 인코딩된 비밀번호
     */
    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BaseException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
