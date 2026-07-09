package com.allegro.backanonymization.exception;

import lombok.Getter;

/**
 * 애플리케이션의 기본 예외입니다.
 */
@Getter
public class BaseException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * 새로운 BaseException을 생성합니다.
     *
     * @param errorCode 오류 코드
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}