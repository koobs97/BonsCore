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

    INTERNAL_SERVER_ERROR("ER_000", "서버 내부에서 에러가 발생했습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
