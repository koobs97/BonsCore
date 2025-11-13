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
    UNAUTHORIZED("ER_004", "권한이 없습니다."), // 일반적인 권한 없음
    DUPLICATE_LOGIN("ER_104", "중복 로그인이 감지되어 강제 로그아웃 됩니다."),
    ACCESS_DENIED("ER_105", "이 리소스에 접근할 수 있는 권한이 없습니다."), // 특정 리소스 접근 제어
    ACCOUNT_LOCKED("ER_106", "로그인 시도 횟수 초과로 계정이 잠겼습니다."), // 일반 계정 잠금
    ACCOUNT_LOCKED_WITH_TIMEOUT("ER_109", "로그인 횟수(5회)를 초과하여 계정이 잠겼습니다. 5분 후에 다시 시도해주세요."), // New: 5분 타임아웃 상세
    WEAK_PASSWORD("ER_107", "이 비밀번호는 유출된 이력이 있어 사용할 수 없습니다."), // 유출 비밀번호 (일반)
    WEAK_PASSWORD_USE_ANOTHER("ER_110", "이 비밀번호는 유출된 이력이 있어 사용할 수 없습니다. 다른 비밀번호를 사용해주세요."), // New: 유출 비밀번호 (다른 비밀번호 사용 권고)
    DORMANT_USER_NOT_FOUND("ER_108", "휴면 상태 정보를 찾을 수 없습니다"), // USER_NOT_FOUND에서 이름 변경, 좀 더 구체적
    RECAPTCHA_REQUIRED("ER_111", "계정 보안을 위해 reCAPTCHA 인증이 필요합니다."), // New
    RECAPTCHA_FAILED("ER_112", "자동 입력 방지 문자 확인에 실패했습니다."), // New
    LOGGED_IN_ON_ANOTHER_DEVICE("ER_113", "다른 기기에서 로그인 중입니다.<br>접속을 강제로 끊고 로그인하시겠습니까?"), // New
    ACCESS_DENIED_GENERAL("ER_114", "접속권한이 없습니다."), // New: 일반적인 접속 권한 없음 (ER_105와 구분)
    ACCOUNT_DORMANT_LONG_INACTIVE("ER_115", "장기간 미접속으로 계정이 휴면 상태로 전환되었습니다.<br>본인인증을 통해 계정을 활성화할 수 있습니다."), // New
    UNUSUAL_LOGIN_DETECTED("ER_116", "다른 환경에서의 로그인이 감지되었습니다.<br>계정 보호를 위해 본인인증을 진행해주세요."), // New
    USERNAME_REQUIRED("ER_117", "유저명을 입력해주세요."),
    EMAIL_REQUIRED("ER_118", "이메일을 입력해주세요."),
    USER_INFO_NOT_MATCH("ER_119", "입력하신 정보와 일치하는 사용자가 없습니다."), // New: 사용자 정보 불일치
    USERID_REQUIRED("ER_120", "아이디를 입력해주세요."),
    AUTH_TYPE_NOT_SET("ER_121", "인증타입이 설정되지 않았습니다."), // New
    AUTH_CODE_INVALID_OR_EXPIRED("ER_122", "인증 코드가 유효하지 않거나 만료되었습니다."), // New
    INCORRECT_ANSWER("ER_123", "답변이 올바르지 않습니다."), // New
    TOKEN_INVALID_OR_EXPIRED("ER_124", "토큰이 유효하지 않거나 만료되었습니다."), // New

    // 로그인 관련 에러
    INVALID_CREDENTIALS("ER_005", "잘못된 사용자 이름 또는 비밀번호입니다."),
    LOGIN_FAILED("ER_006", "로그인에 실패했습니다."),

    // 리프레시 토큰 갱신 관련 에러
    REFRESH_TOKEN_EXPIRED("ER_007", "리프레시 토큰이 만료되었습니다."), // 기존 ER_007
    REFRESH_TOKEN_NOT_FOUND("ER_008", "리프레시 토큰을 찾을 수 없습니다."), // 기존 ER_008
    PREVENT_DOUBLE_REQUEST("ER_009", "이미 처리 중인 요청입니다. 잠시 후 다시 시도해주세요."), // 기존 ER_009

    INVALID_INPUT("ER_010", "값이 유효하지 않습니다."), // ER_009 중복 코드 수정 (새로운 ER_010)
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
