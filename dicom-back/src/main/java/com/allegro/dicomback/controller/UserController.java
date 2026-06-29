package com.allegro.dicomback.controller;

import com.allegro.dicomback.dto.UserDto.*;
import com.allegro.dicomback.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        userService.logout(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/check-id")
    public ResponseEntity<Boolean> checkId(@RequestBody IdCheckRequest request) {
        return ResponseEntity.ok(userService.checkIdDuplicate(request.id()));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestHeader("Authorization") String token, @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request); // 토큰 검증은 인터셉터나 시큐리티에서 처리 가정
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser(@RequestHeader("Authorization") String token, @RequestBody DeleteUserRequest request) {
        userService.deleteUser(request);
        return ResponseEntity.noContent().build();
    }
}