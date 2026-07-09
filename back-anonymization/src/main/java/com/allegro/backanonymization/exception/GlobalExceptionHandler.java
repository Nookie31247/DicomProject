package com.allegro.backanonymization.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

/**
 * 전역 예외 처리기입니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    // TODO 나중에 println 지우기!!
    /**
     * BaseException을 처리합니다.
     *
     * @param e BaseException
     * @return 오류 응답을 포함하는 ResponseEntity
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        System.out.println(e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 일반적인 Exception을 처리합니다.
     *
     * @param e Exception
     * @return 오류 응답을 포함하는 ResponseEntity
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