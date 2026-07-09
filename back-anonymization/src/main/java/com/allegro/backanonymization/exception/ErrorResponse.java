package com.allegro.backanonymization.exception;

/**
 * 오류 응답을 나타내는 레코드입니다.
 */
public record ErrorResponse(String code, String message) {
    /**
     * ErrorCode에서 ErrorResponse를 생성합니다.
     *
     * @param errorCode 오류 코드
     * @return 새로운 ErrorResponse
     */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}