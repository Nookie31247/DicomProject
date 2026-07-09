package com.allegro.dicomback.dto;

import java.time.LocalDate;

/**
 * 사용자 관련 응답을 위한 데이터 전송 객체(DTO)입니다.
 */
public class UserResponseDto {

    /**
     * 성공적인 로그인 응답을 위한 DTO입니다.
     *
     * @param username 로그인한 사용자의 이름
     */
    public record LoginRes(String username) {}

    /**
     * ID가 고유한지 여부를 나타내는 ID 확인 응답을 위한 DTO입니다.
     *
     * @param isUnique ID를 사용할 수 있으면 true, 그렇지 않으면 false
     */
    public record CheckIdRes(Boolean isUnique) {}

    /**
     * 사용자의 상세 정보를 반환하기 위한 DTO입니다.
     *
     * @param userId 사용자의 ID
     * @param username 사용자의 이름
     * @param registerDay 사용자가 등록된 날짜
     */
    public record UserInfoRes(String userId, String username, LocalDate registerDay) {}
}
