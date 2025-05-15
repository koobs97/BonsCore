package com.koo.bonscore.core.config.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Header header;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(new Header(true, "SUCCESS"), message, data);
    }

    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(new Header(false, code), message, data);
    }

    @Getter
    @AllArgsConstructor
    public static class Header {
        private boolean success;
        private String code;
    }
}
