package com.koo.bonscore.core.exception.enumType;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전용 http 상태코드 enum
 *
 * @author bons
 * @version 1.0
 * @since 2025-01-10
 */

@Getter
public enum HttpStatusCode {
    OK(HttpStatus.OK),
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    HttpStatusCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}

