package com.hmdp.config;

import com.hmdp.utils.BaseInterceptor;
import com.hmdp.utils.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new BaseInterceptor());

        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/user/login",
                "/user/code",
                "/blog/**",
                "/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**"
        );
    }
}
