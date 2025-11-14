package com.koo.bonscore.core.exception.custom;


import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전용 예외 클래스.
 *
 * @author bons
 * @version 1.0
 * @since 2025-01-13
 */
@Getter
public class BsCoreException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final ErrorCode errorCode;

    // 기본 메시지 사용 처리
    public BsCoreException(HttpStatusCode statusCode, ErrorCode errorCode) {
        super(errorCode.getMessage()); // 기본 메시지 사용
        this.httpStatus = statusCode.getHttpStatus();
        this.errorCode = errorCode;
    }

    // customMessage를 옵션으로 받아 처리 (없으면 null 허용)
    public BsCoreException(HttpStatusCode statusCode, ErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.httpStatus = statusCode.getHttpStatus();
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode를 기반으로 예외를 생성합니다.
     * ErrorCode에 정의된 기본 HTTP 상태 코드와 메시지를 사용합니다.
     * @param errorCode 사용할 ErrorCode Enum
     */
    public BsCoreException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        // ErrorCode에 새로 추가된 status 필드를 사용
        this.httpStatus = errorCode.getStatus();
    }

    /**
     * ErrorCode와 사용자 정의 메시지를 기반으로 예외를 생성합니다.
     * @param errorCode 사용할 ErrorCode Enum
     * @param customMessage 재정의할 예외 메시지
     */
    public BsCoreException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
        // ErrorCode에 새로 추가된 status 필드를 사용
        this.httpStatus = errorCode.getStatus();
    }

    public String getErrorCode() {
        return errorCode.getCode();
    }

    // getStatusCode() 같은 메서드가 필요하다면 HttpStatus를 반환하도록 추가
    public HttpStatus getStatusCode() {
        return httpStatus;
    }

}
