package com.koo.bonscore.core.config.api;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ApiResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,  @Nullable Class<? extends HttpMessageConverter<?>> converterType) {
        // 이미 ApiResponse인 경우는 감싸지 않음
        return !returnType.getParameterType().equals(ApiResponse.class)
                && !returnType.getParameterType().equals(String.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  @Nullable MethodParameter returnType,
                                  @Nullable MediaType selectedContentType,
                                  @Nullable Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @Nullable ServerHttpRequest request,
                                  @Nullable ServerHttpResponse response) {
        return ApiResponse.success("요청 성공", body);
    }

}
