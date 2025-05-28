package com.koo.bonscore.core.config.web;

import com.koo.bonscore.common.paging.interceptor.PageClearInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private PageClearInterceptor pageClearInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(pageClearInterceptor)
                .addPathPatterns("/**");  // 전체 경로에 적용
    }
}
