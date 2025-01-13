package com.koo.bonscore.core.exception.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 에러 응답 구조
 *
 * @author bons
 * @version 1.0
 * @since 2025-01-13
 */

@AllArgsConstructor
@Getter
@Setter
public class ErrorResponse {
    private LocalDateTime timestamp; // 발생 시각
    private int status;              // HTTP 상태 코드
    private String error;            // HTTP 상태 설명
    private String code;             // 애플리케이션 에러 코드
    private String message;          // 에러 메시지
    private String path;             // 요청 경로 (선택)
}

