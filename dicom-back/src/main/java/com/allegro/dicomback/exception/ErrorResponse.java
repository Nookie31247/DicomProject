package com.allegro.dicomback.exception;

/**
 * 오류 응답을 나타내는 레코드입니다.
 *
 * @param code 오류 코드
 * @param message 오류 메시지
 */
public record ErrorResponse(String code, String message) {
    /**
     * ErrorCode에서 ErrorResponse를 생성합니다.
     *
     * @param errorCode ErrorCode
     * @return ErrorResponse
     */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}