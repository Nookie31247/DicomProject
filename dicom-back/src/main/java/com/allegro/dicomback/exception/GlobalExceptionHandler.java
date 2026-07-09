package com.allegro.dicomback.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

/**
 * 애플리케이션의 전역 예외 핸들러입니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    // TODO 나중에 println 지우기!!
    /**
     * 비즈니스 예외를 처리합니다.
     * 우리가 정의한 비즈니스 예외 처리
     *
     * @param e BaseException
     * @return 오류 응답 엔티티
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        String original = e.getMessage();
        String truncated = original.substring(0, Math.min(original.length(), 1000));
        System.out.println(truncated);
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 일반적인 예외를 처리합니다.
     * 서버 내부 예외 처리 (예상치 못한 에러)
     *
     * @param e Exception
     * @return 오류 응답 엔티티
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        Arrays.stream(e.getStackTrace()).forEach(System.out::println);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }
}