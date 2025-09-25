package com.koo.bonscore.core.config.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <pre>
 * ApiResponse.java
 * 설명 : API 응답을 위한 표준 래퍼 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-13
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Header header;      // 응답의 성공 여부와 상태 코드를 담는 헤더 객체
    private String message;     // 응답 결과에 대한 사용자 메시지
    private T data;             // 실제 응답 데이터를 담는 페이로드

    /**
     * 성공 응답 객체를 생성
     * 성공 시 헤더의 상태 코드는 기본값 "SUCCESS"로 설정
     *
     * @param message   성공 메시지
     * @param data      반환할 데이터 페이로드
     * @param <T>       응답 데이터의 타입
     * @return          성공 상태를 나타내는 {@code ApiResponse} 객체
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(new Header(true, "SUCCESS"), message, data);
    }

    /**
     * 실패 응답 객체를 생성
     *
     * @param code      커스텀 에러 코드
     * @param message   실패/에러 메시지
     * @param data      null
     * @param <T>       null
     * @return          실패 상태를 나타내는 {@code ApiResponse} 객체
     */
    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(new Header(false, code), message, data);
    }

    /**
     * API 응답의 메타데이터를 담는 내부 정적 클래스
     */
    @Getter
    @AllArgsConstructor
    public static class Header {
        private boolean success;
        private String code;
    }
}
