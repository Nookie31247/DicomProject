package com.allegro.dicomback.exception;

import lombok.Getter;

/**
 * 애플리케이션의 기본 예외 클래스입니다.
 */
@Getter
public class BaseException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * 지정된 ErrorCode로 새로운 BaseException을 구성합니다.
     *
     * @param errorCode 오류 코드
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}