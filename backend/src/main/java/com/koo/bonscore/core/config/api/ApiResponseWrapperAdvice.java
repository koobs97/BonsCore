package com.koo.bonscore.core.config.api;

import jakarta.annotation.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

@RestControllerAdvice
public class ApiResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 컨트롤러 메서드의 반환 타입이 {@link ApiResponse} 또는 {@link String}인 경우에는
     * 응답을 감싸지 않도록 처리.
     *
     * @param returnType   컨트롤러 메서드의 반환 타입 정보
     * @param converterType 선택된 메시지 컨버터 타입
     * @return 응답을 감쌀지 여부 (true 면 감싸고, false 면 감싸지 않음)
     */
    @Override
    public boolean supports(MethodParameter returnType,  @Nullable Class<? extends HttpMessageConverter<?>> converterType) {

        if (ResourceHttpRequestHandler.class.isAssignableFrom(returnType.getContainingClass())) {
            return false;
        }

        // 이미 ApiResponse인 경우는 감싸지 않음
        Class<?> parameterType = returnType.getParameterType();
        return !parameterType.equals(ApiResponse.class)
                && !parameterType.equals(String.class)
                && !parameterType.equals(Void.TYPE);
    }

    /**
     * 컨트롤러 응답 바디를 가로채어 {@link ApiResponse} 형태로 감싼다.
     * <p>
     * 만약 이미 {@code ApiResponse}이고 내부 {@code header.success}가 false인 경우
     * (즉, 에러 응답)에는 감싸지 않고 그대로 반환.
     *
     * @param body                 원본 응답 바디
     * @param returnType           메서드 반환 타입
     * @param selectedContentType  응답 콘텐츠 타입
     * @param selectedConverterType 선택된 메시지 컨버터 타입
     * @param request              HTTP 요청 정보
     * @param response             HTTP 응답 정보
     * @return 감싼 응답 또는 원본 응답
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  @Nullable MethodParameter returnType,
                                  @Nullable MediaType selectedContentType,
                                  @Nullable Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @Nullable ServerHttpRequest request,
                                  @Nullable ServerHttpResponse response) {

        // body가 ApiResponse의 인스턴스이고 header의 success가 "false" 인경우
        if (body instanceof ApiResponse<?> api) {
            if (!api.getHeader().isSuccess()) {
                return body;
            }
        }

        return ApiResponse.success("요청 성공", body);
    }

}
