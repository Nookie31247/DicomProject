package com.allegro.dicomback.dto;

/**
 * 사용자 관련 요청을 위한 데이터 전송 객체(DTO)입니다.
 */
public class UserRequestDto {

    /**
     * 사용자 로그인 요청을 위한 DTO입니다.
     *
     * @param userId 사용자의 ID
     * @param password 사용자의 비밀번호
     */
    public record LoginRequest(String userId, String password) {}

    /**
     * 사용자 회원가입 요청을 위한 DTO입니다.
     *
     * @param userId 사용자의 ID
     * @param password 사용자의 비밀번호
     * @param name 사용자의 이름
     */
    public record SignupRequest(String userId, String password, String name) {}

    /**
     * 사용자 ID가 사용 가능한지 확인하기 위한 DTO입니다.
     *
     * @param userId 확인할 사용자 ID
     */
    public record IdCheckRequest(String userId) {}

    /**
     * 사용자의 비밀번호를 변경하기 위한 DTO입니다.
     *
     * @param currentPassword 사용자의 현재 비밀번호
     * @param newPassword 설정할 새 비밀번호
     */
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    /**
     * 사용자 계정을 삭제하기 위한 DTO입니다.
     *
     * @param password 확인을 위한 사용자의 비밀번호
     */
    public record DeleteUserRequest(String password) {}
}