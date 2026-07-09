package com.allegro.dicomback.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 유저 관련 (1xxx)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "유저를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "USER_002", "비밀번호가 일치하지 않습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_003", "이미 존재하는 아이디입니다."),
    INVALID_USER_TYPE(HttpStatus.BAD_REQUEST, "USER_004", "올바르지 않은 회원 유형입니다."),

    // 환자 연동 관련
    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PATIENT_001", "환자를 찾을 수 없습니다."),

    // DICOM/데이터 관련 (2xxx)
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "DICOM_001", "해당 스터디를 찾을 수 없습니다."),
    SERIES_NOT_FOUND(HttpStatus.NOT_FOUND, "DICOM_003", "해당 시리즈를 찾을 수 없습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "DICOM_004", "해당 이미지를 찾을 수 없습니다."),
    FILE_NOT_FOUND_ON_DISK(HttpStatus.INTERNAL_SERVER_ERROR, "DICOM_005", "원본 파일을 디스크에서 찾을 수 없습니다."),
    STUDY_NOT_SYNCED(HttpStatus.CONFLICT, "DICOM_005", "해당 스터디가 아직 PACS와 동기화되지 않았습니다."),
    SERIES_NOT_SYNCED(HttpStatus.CONFLICT, "DICOM_006", "해당 시리즈가 아직 PACS와 동기화되지 않았습니다."),
    EMPTY_DOWNLOAD_SELECTION(HttpStatus.BAD_REQUEST, "DICOM_007", "다운로드할 항목이 선택되지 않았습니다."),

    // JWT 관련 (3xxx)
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_001", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_002", "유효하지 않은 토큰입니다."),
    EMPTY_TOKEN(HttpStatus.BAD_REQUEST, "JWT_003", "토큰이 존재하지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.BAD_REQUEST, "JWT_004", "지원되지 않는 형식의 토큰입니다."),

    // 서버 기본
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_001", "서버 내부 오류입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}