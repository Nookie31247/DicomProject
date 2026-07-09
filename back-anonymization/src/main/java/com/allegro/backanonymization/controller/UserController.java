package com.allegro.backanonymization.controller;

import com.allegro.backanonymization.dto.UserRequestDto.*;
import com.allegro.backanonymization.dto.UserResponseDto.*;
import com.allegro.backanonymization.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/research/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        UserService.LoginServiceRes serviceRes = userService.login(request);

        // httpOnly 쿠키 생성
        ResponseCookie cookie = ResponseCookie.from("token", serviceRes.token())
                .httpOnly(true)         // 브라우저가 쿠키를 읽을 수 없도록 설정
                .secure(false)          // HTTP 환경에서만 전송 (일단 false로 해둠)
                .path("/")
                .maxAge(3600 * 24)   // 쿠키 만료 시간: JWT 토큰 만료 시간과 맞춰서 설정 (24시간)
                .sameSite("Lax")        // 다른 사이트에서 넘어올 때 안전한 요청만 쿠키를 허용하는 설정
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new LoginRes(serviceRes.username()));
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "token") String token, HttpServletResponse response) {
        // 만료 시간이 0인 쿠키를 넘겨주어 브라우저가 쿠키를 삭제하도록 유도
        ResponseCookie cookie = ResponseCookie.from("token", "")
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .maxAge(0)
                        .sameSite("Lax")
                        .build();

        userService.logout(token);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/check-id")
    public ResponseEntity<CheckIdRes> checkId(@RequestBody IdCheckRequest request) {
        return ResponseEntity.ok(new CheckIdRes(userService.checkIdDuplicate(request.userId())));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@CookieValue(name = "token") String token, @RequestBody ChangePasswordRequest request) {
        userService.changePassword(token, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> Withdraw(@CookieValue(name = "token") String token, @RequestBody DeleteUserRequest request) {
        userService.deleteUser(token, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/info")
    public ResponseEntity<UserInfoRes> getUserInfo(@CookieValue(name = "token") String token) {
        return ResponseEntity.ok(userService.getUserInfo(token));
    }
}
