package com.nfu.pettravel.config;

import com.nfu.pettravel.interceptor.JwtTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtTokenInterceptor jwtTokenInterceptor;

    public WebMvcConfiguration(JwtTokenInterceptor jwtTokenInterceptor) {
        this.jwtTokenInterceptor = jwtTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/api/**") // 拦截所有 /api 开头的请求
                .excludePathPatterns("/api/user/login") // 放行登录接口
                .excludePathPatterns("/api/user/register")
                .excludePathPatterns("/doc.html", "/webjars/**", "/v3/api-docs/**"); // 放行 Knife4j 文档资源
    }
}
