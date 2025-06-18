package com.koo.bonscore.core.config.web;

import com.koo.bonscore.common.paging.interceptor.PageClearInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PageClearInterceptor pageClearInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pageClearInterceptor)
                .addPathPatterns("/**");  // 전체 경로에 적용
    }
}
