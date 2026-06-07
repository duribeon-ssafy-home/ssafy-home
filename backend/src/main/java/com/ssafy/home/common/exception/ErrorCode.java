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

    //Property Image
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지는 최대 10장까지 등록할 수 있습니다."),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
    PUBLIC_PROPERTY_IMAGE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "공공 데이터 매물은 이미지를 등록할 수 없습니다."),

    // Favorite
    FAVORITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 관심 매물로 등록된 매물입니다."),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "관심 매물 목록에 없는 매물입니다."),

    // Lifestyle
    LIFESTYLE_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "생활 성향 결과를 찾을 수 없습니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
