package com.ssafy.home.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Property
    PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "매물을 찾을 수 없습니다."),
    PROPERTY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 매물에 대한 권한이 없습니다."),
    PROPERTY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 매물입니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
