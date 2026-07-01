package com.allegro.dicomback.dto;

import java.time.LocalDate;

public class UserResponseDto {
    public record LoginRes(String username) {}
    public record CheckIdRes(Boolean isUnique) {}
    public record UserInfoRes(String userId, String username, String userRole, LocalDate registerDay) {}
}
