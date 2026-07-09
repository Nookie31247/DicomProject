package com.allegro.backanonymization.dto;

import java.time.LocalDate;

/**
 * 사용자 응답을 위한 DTO입니다.
 */
public class UserResponseDto {
    /**
     * 로그인 응답을 위한 DTO입니다.
     */
    public record LoginRes(String username) {}
    /**
     * ID 확인 응답을 위한 DTO입니다.
     */
    public record CheckIdRes(Boolean isUnique) {}

    /**
     * 사용자 정보 응답을 위한 DTO입니다.
     */
    public record UserInfoRes(String userId, String username, LocalDate registerDay) {}
}
