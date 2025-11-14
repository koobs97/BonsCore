package com.koo.bonscore.core.exception.enumType;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전용 에러코드 enum
 *
 * @author bons
 * @version 1.0
 * @since 2025-01-10
 */

@Getter
public enum ErrorCode {

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ER_000", "서버 내부에서 에러가 발생했습니다."),

    // 401 Unauthorized
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "ER_001", "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "ER_002", "엑세스 토큰이 만료되었습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "ER_003", "엑세스 토큰이 유효하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ER_004", "권한이 없습니다."),
    DUPLICATE_LOGIN(HttpStatus.UNAUTHORIZED, "ER_104", "중복 로그인이 감지되어 강제 로그웃 됩니다."),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "ER_106", "로그인 시도 횟수 초과로 계정이 잠겼습니다."),
    ACCOUNT_LOCKED_WITH_TIMEOUT(HttpStatus.UNAUTHORIZED, "ER_109", "로그인 횟수(5회)를 초과하여 계정이 잠겼습니다. 5분 후에 다시 시도해주세요."),
    DORMANT_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "ER_108", "휴면 상태 정보를 찾을 수 없습니다"),
    RECAPTCHA_REQUIRED(HttpStatus.UNAUTHORIZED, "ER_111", "계정 보안을 위해 reCAPTCHA 인증이 필요합니다."),
    RECAPTCHA_FAILED(HttpStatus.UNAUTHORIZED, "ER_112", "자동 입력 방지 문자 확인에 실패했습니다."),
    LOGGED_IN_ON_ANOTHER_DEVICE(HttpStatus.UNAUTHORIZED, "ER_113", "다른 기기에서 로그인 중입니다.<br>접속을 강제로 끊고 로그인하시겠습니까?"),
    ACCOUNT_DORMANT_LONG_INACTIVE(HttpStatus.UNAUTHORIZED, "ER_115", "장기간 미접속으로 계정이 휴면 상태로 전환되었습니다.<br>본인인증을 통해 계정을 활성화할 수 있습니다."),
    UNUSUAL_LOGIN_DETECTED(HttpStatus.UNAUTHORIZED, "ER_116", "다른 환경에서의 로그인이 감지되었습니다.<br>계정 보호를 위해 본인인증을 진행해주세요."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "ER_007", "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "ER_008", "리프레시 토큰을 찾을 수 없습니다."),
    TOKEN_INVALID_OR_EXPIRED(HttpStatus.UNAUTHORIZED, "ER_124", "토큰이 유효하지 않거나 만료되었습니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "ER_125", "세션이 만료되었습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ER_005", "잘못된 사용자 이름 또는 비밀번호입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "ER_006", "로그인에 실패했습니다."),

    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ER_105", "이 리소스에 접근할 수 있는 권한이 없습니다."),
    ACCESS_DENIED_GENERAL(HttpStatus.FORBIDDEN, "ER_114", "접속권한이 없습니다."),

    // 400 Bad Request
    WEAK_PASSWORD(HttpStatus.BAD_REQUEST, "ER_107", "이 비밀번호는 유출된 이력이 있어 사용할 수 없습니다."),
    WEAK_PASSWORD_USE_ANOTHER(HttpStatus.BAD_REQUEST, "ER_110", "이 비밀번호는 유출된 이력이 있어 사용할 수 없습니다. 다른 비밀번호를 사용해주세요."),
    USERNAME_REQUIRED(HttpStatus.BAD_REQUEST, "ER_117", "유저명을 입력해주세요."),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "ER_118", "이메일을 입력해주세요."),
    USER_INFO_NOT_MATCH(HttpStatus.BAD_REQUEST, "ER_119", "입력하신 정보와 일치하는 사용자가 없습니다."),
    USERID_REQUIRED(HttpStatus.BAD_REQUEST, "ER_120", "아이디를 입력해주세요."),
    AUTH_TYPE_NOT_SET(HttpStatus.BAD_REQUEST, "ER_121", "인증타입이 설정되지 않았습니다."),
    AUTH_CODE_INVALID_OR_EXPIRED(HttpStatus.BAD_REQUEST, "ER_122", "인증 코드가 유효하지 않거나 만료되었습니다."),
    INCORRECT_ANSWER(HttpStatus.BAD_REQUEST, "ER_123", "답변이 올바르지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "ER_010", "값이 유효하지 않습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "F001", "허용되지 않는 파일 확장자입니다."),

    // 409 Conflict
    PREVENT_DOUBLE_REQUEST(HttpStatus.CONFLICT, "ER_009", "이미 처리 중인 요청입니다. 잠시 후 다시 시도해주세요."),

    // 503 Service Unavailable (예시)
    FILE_STORAGE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "F002", "파일 저장에 실패했습니다."),
    KAKAO_API_FAILURE(HttpStatus.SERVICE_UNAVAILABLE, "EX_001", "외부 API 연동 중 오류가 발생했습니다. (카카오 지도)");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) { // 생성자 수정
        this.status = status;
        this.code = code;
        this.message = message;
    }

}
