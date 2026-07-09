package com.allegro.backanonymization.dto;

/**
 * 사용자 요청을 위한 DTO입니다.
 */
public class UserRequestDto {
    /**
     * 로그인 요청을 위한 DTO입니다.
     */
    public record LoginRequest(String userId, String password) {}

    /**
     * 회원가입 요청을 위한 DTO입니다.
     */
    public record SignupRequest(String userId, String password, String name) {}

    /**
     * ID 확인 요청을 위한 DTO입니다.
     */
    public record IdCheckRequest(String userId) {}

    /**
     * 비밀번호 변경 요청을 위한 DTO입니다.
     */
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    /**
     * 사용자 삭제 요청을 위한 DTO입니다.
     */
    public record DeleteUserRequest(String password) {}
}
