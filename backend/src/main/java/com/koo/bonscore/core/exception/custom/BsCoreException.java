package com.koo.bonscore.core.exception.custom;


import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 전용 예외 클래스.
 *
 * @author bons
 * @version 1.0
 * @since 2025-01-13
 */

@Getter
public class BsCoreException extends RuntimeException {
    private final HttpStatusCode statusCode;
    private final ErrorCode errorCode;

    // 기본 메시지 사용 처리
    public BsCoreException(HttpStatusCode statusCode, ErrorCode errorCode) {
        super(errorCode.getMessage()); // 기본 메시지 사용
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    // customMessage를 옵션으로 받아 처리 (없으면 null 허용)
    public BsCoreException(HttpStatusCode statusCode, ErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode.getCode();
    }

}
