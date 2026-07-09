package com.allegro.backanonymization.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 오류 코드의 열거형입니다.
 */
@Getter
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "유저를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "USER_002", "비밀번호가 일치하지 않습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_003", "이미 존재하는 아이디입니다."),

    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PATIENT_001", "환자를 찾을 수 없습니다."),

    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "DICOM_001", "해당 스터디를 찾을 수 없습니다."),
    SERIES_NOT_FOUND(HttpStatus.NOT_FOUND, "DICOM_003", "해당 시리즈를 찾을 수 없습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "DICOM_004", "해당 이미지를 찾을 수 없습니다."),
    FILE_NOT_FOUND_ON_DISK(HttpStatus.INTERNAL_SERVER_ERROR, "DICOM_005", "원본 파일을 디스크에서 찾을 수 없습니다."),
    STUDY_NOT_SYNCED(HttpStatus.CONFLICT, "DICOM_005", "해당 스터디가 아직 PACS와 동기화되지 않았습니다."),
    SERIES_NOT_SYNCED(HttpStatus.CONFLICT, "DICOM_006", "해당 시리즈가 아직 PACS와 동기화되지 않았습니다."),
    EMPTY_DOWNLOAD_SELECTION(HttpStatus.BAD_REQUEST, "DICOM_007", "다운로드할 항목이 선택되지 않았습니다."),

    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_001", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_002", "유효하지 않은 토큰입니다."),
    EMPTY_TOKEN(HttpStatus.BAD_REQUEST, "JWT_003", "토큰이 존재하지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.BAD_REQUEST, "JWT_004", "지원되지 않는 형식의 토큰입니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_001", "서버 내부 오류입니다.");

    /**
     * 오류와 연결된 HTTP 상태입니다.
     */
    private final HttpStatus status;

    /**
     * 고유한 오류 코드 문자열입니다.
     */
    private final String code;

    /**
     * 설명적인 오류 메시지입니다.
     */
    private final String message;

    /**
     * 새로운 ErrorCode를 생성합니다.
     *
     * @param status HTTP 상태
     * @param code 오류 코드 문자열
     * @param message 오류 메시지
     */
    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
