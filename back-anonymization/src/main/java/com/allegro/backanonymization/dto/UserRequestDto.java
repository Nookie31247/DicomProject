package com.allegro.backanonymization.dto;

public class UserRequestDto {
    // 로그인
    public record LoginRequest(String userId, String password) {}

    // 회원가입
    public record SignupRequest(String userId, String password, String name, String userType) {}

    // 아이디 중복 확인(굳이 이거 안쓰고 String으로 해도 됨)
    public record IdCheckRequest(String userId) {}

    // 비밀번호 수정
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    // 회원탈퇴
    public record DeleteUserRequest(String password) {}
}