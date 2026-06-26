package com.allegro.dicomback.dto;

public class UserDto {
    // 로그인
    public record LoginRequest(String userId, String password) {}
    public record TokenResponse(String token) {}

    // 회원가입
    public record SignupRequest(String userId, String password, String name, String userRole) {}

    // 아이디 중복 확인(굳이 이거 안쓰고 String으로 해도 됨)
    public record IdCheckRequest(String id) {}

    // 비밀번호 수정
    public record ChangePasswordRequest(String userId, String currentPassword, String newPassword) {}

    // 회원탈퇴
    public record DeleteUserRequest(String userId, String currentPassword) {}
}