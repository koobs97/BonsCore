package com.koo.bonscore.core.exception.enumType;

import lombok.Getter;
import org.springframework.http.HttpStatus;

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

