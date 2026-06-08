package com.ssafy.home.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INACTIVE_USER(HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
    BANNED_USER(HttpStatus.FORBIDDEN, "이용 제한된 계정입니다."),
    DELETED_USER(HttpStatus.FORBIDDEN, "탈퇴 처리된 계정입니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "허용되지 않은 역할입니다."),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "허용되지 않은 계정 상태입니다."),
    PHONE_NUMBER_REQUIRED(HttpStatus.BAD_REQUEST, "전화번호는 필수입니다."),

    // Property
    PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "매물을 찾을 수 없습니다."),
    PROPERTY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 매물에 대한 권한이 없습니다."),
    PROPERTY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 매물입니다."),

    // Report
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신고한 매물입니다."),

    // Lifestyle
    LIFESTYLE_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "생활 성향 결과를 찾을 수 없습니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
