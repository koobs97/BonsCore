package com.koo.bonscore.core.exception.enumType;

import lombok.Getter;

/**
 * 애플리케이션 전용 에러코드 enum
 *
 * @author bons
 * @version 1.0
 * @since 2025-01-10
 */

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR("ER_000", "서버 내부에서 에러가 발생했습니다."),

    // 인증 관련 에러
    INVALID_REFRESH_TOKEN("ER_001", "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_ACCESS_TOKEN("ER_002", "엑세스 토큰이 만료되었습니다."),
    INVALID_ACCESS_TOKEN("ER_003", "엑세스 토큰이 유효하지 않습니다."),
    UNAUTHORIZED("ER_004", "권한이 없습니다."),
    DUPLICATE_LOGIN("ER_104", "중복 로그인이 감지되어 강제 로그아웃 됩니다."),

    // 로그인 관련 에러
    INVALID_CREDENTIALS("ER_005", "잘못된 사용자 이름 또는 비밀번호입니다."),
    LOGIN_FAILED("ER_006", "로그인에 실패했습니다."),

    // 리프레시 토큰 갱신 관련 에러
    REFRESH_TOKEN_EXPIRED("ER_007", "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_NOT_FOUND("ER_008", "리프레시 토큰을 찾을 수 없습니다."),
    PREVENT_DOUBLE_REQUEST("ER_009", "이미 처리 중인 요청입니다. 잠시 후 다시 시도해주세요."),

    INVALID_INPUT("ER_009", "값이 유효하지 않습니다."),
    INVALID_FILE_EXTENSION("F001", "허용되지 않는 파일 확장자입니다."),
    FILE_STORAGE_FAILED("F002", "파일 저장에 실패했습니다.");
    ;

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
