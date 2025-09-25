package com.koo.bonscore.core.config.web;

import com.koo.bonscore.common.paging.interceptor.PageClearInterceptor;
import com.koo.bonscore.log.interceptor.UserActivityLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * <pre>
 * WebConfig.java
 * 설명 : Spring Web MVC의 핵심 설정을 커스터마이징하는 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-29
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PageClearInterceptor pageClearInterceptor;
    private final UserActivityLogInterceptor userActivityLogInterceptor;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.resource-url-prefix}")
    private String resourceUrlPrefix;

    /**
     * 애플리케이션의 인터셉터 체인(Interceptor Chain)에 커스텀 인터셉터를 등록.
     * 등록된 순서대로 인터셉터가 실행된다.
     *
     * @param registry 인터셉터를 등록하고 URL 패턴을 매핑하는 데 사용되는 등록기(registry) 객체
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. PageClearInterceptor: 모든 경로(/**)에 적용
        registry.addInterceptor(pageClearInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(resourceUrlPrefix + "**");

        // 2. UserActivityLogInterceptor: '/api/auth/**' 경로에만 적용
        registry.addInterceptor(userActivityLogInterceptor)
                .addPathPatterns("/api/auth/**");
    }

    /**
     * 정적 파일 리소스 핸들러 등록
     * 지정된 URL 패턴(resourceUrlPrefix + "**")으로 들어오는 요청에 대해
     * 서버의 업로드 디렉토리(uploadDir)에서 파일을 찾아 제공한다.
     *
     * @param registry ResourceHandlerRegistry 객체
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /images/** URL 요청이 오면 file:./uploads/ 경로에서 파일을 찾아 제공
        registry.addResourceHandler(resourceUrlPrefix + "**")
                .addResourceLocations("file:" + uploadDir);
    }
}
