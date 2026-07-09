package com.allegro.dicomback.controller;

import com.allegro.dicomback.dto.UserRequestDto.*;
import com.allegro.dicomback.dto.UserResponseDto.*;
import com.allegro.dicomback.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 로그인, 회원가입 및 프로필 관리와 같은 사용자 작업을 관리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/medical/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자를 인증하고 JWT 쿠키를 설정합니다.
     *
     * @param request 로그인 자격 증명
     * @param response 쿠키를 설정하기 위한 HTTP 응답
     * @return 로그인 응답을 포함하는 ResponseEntity
     */
    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        UserService.LoginServiceRes serviceRes = userService.login(request);

        // httpOnly 쿠키 생성
        ResponseCookie cookie = ResponseCookie.from("token", serviceRes.token())
                .httpOnly(true)         // 브라우저가 쿠키를 읽을 수 없도록 설정
                .secure(false)          // HTTP 환경에서만 전송 (일단 false로 해둠)
                .path("/")
                .maxAge(3600 * 24)      // 쿠키 만료 시간: JWT 토큰 만료 시간과 맞춰서 설정 (24시간)
                .sameSite("Lax")        // 다른 사이트에서 넘어올 때 안전한 요청만 쿠키를 허용하는 설정
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new LoginRes(serviceRes.username()));
    }

    /**
     * 새로운 사용자를 등록합니다.
     *
     * @param request 회원가입 세부 정보
     * @return 생성 상태를 포함하는 ResponseEntity
     */
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * JWT 쿠키를 지워 사용자를 로그아웃합니다.
     *
     * @param token JWT 토큰
     * @param response 쿠키를 지우기 위한 HTTP 응답
     * @return no content 상태를 포함하는 ResponseEntity
     */
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

    /**
     * 주어진 사용자 ID가 이미 등록되어 있는지 확인합니다.
     *
     * @param request 사용자 ID를 포함하는 요청
     * @return ID 확인 응답을 포함하는 ResponseEntity
     */
    @PostMapping("/check-id")
    public ResponseEntity<CheckIdRes> checkId(@RequestBody IdCheckRequest request) {
        return ResponseEntity.ok(new CheckIdRes(userService.checkIdDuplicate(request.userId())));
    }

    /**
     * 인증된 사용자의 비밀번호를 변경합니다.
     *
     * @param token JWT 토큰
     * @param request 비밀번호 변경 세부 정보
     * @return no content 상태를 포함하는 ResponseEntity
     */
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@CookieValue(name = "token") String token, @RequestBody ChangePasswordRequest request) {
        userService.changePassword(token, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 계정을 삭제합니다.
     *
     * @param token JWT 토큰
     * @param request 사용자 삭제 세부 정보
     * @return no content 상태를 포함하는 ResponseEntity
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> Withdraw(@CookieValue(name = "token") String token, @RequestBody DeleteUserRequest request) {
        userService.deleteUser(token, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 현재 인증된 사용자의 정보를 검색합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 정보 응답을 포함하는 ResponseEntity
     */
    @GetMapping("/info")
    public ResponseEntity<UserInfoRes> getUserInfo(@CookieValue(name = "token") String token) {
        return ResponseEntity.ok(userService.getUserInfo(token));
    }
}
