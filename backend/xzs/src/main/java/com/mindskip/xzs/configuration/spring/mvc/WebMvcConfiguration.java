package com.mindskip.xzs.configuration.spring.mvc;

import com.mindskip.xzs.configuration.property.SystemConfig;
import com.mindskip.xzs.configuration.spring.wx.TokenHandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;


/**
 * @version 3.5.0
 * @description: The type Web mvc configuration.
 * Copyright (C), 2020-2025, 智慧乐学
 * @date 2021/12/25 9:45
 */
@Configuration
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    private final TokenHandlerInterceptor tokenHandlerInterceptor;
    private final SystemConfig systemConfig;

    /**
     * Instantiates a new Web mvc configuration.
     *
     * @param tokenHandlerInterceptor the token handler interceptor
     * @param systemConfig            the system config
     */
    @Autowired
    public WebMvcConfiguration(TokenHandlerInterceptor tokenHandlerInterceptor, SystemConfig systemConfig) {
        this.tokenHandlerInterceptor = tokenHandlerInterceptor;
        this.systemConfig = systemConfig;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/admin/index.html");
        registry.addRedirectViewController("/student", "/student/index.html");
        registry.addRedirectViewController("/admin", "/admin/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(31556926);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> securityIgnoreUrls = systemConfig.getWx().getSecurityIgnoreUrls();
        // 合并系统级白名单（含 /api/admin/resource/** 等）
        List<String> allIgnores = new java.util.ArrayList<>(securityIgnoreUrls);
        if (systemConfig.getSecurityIgnoreUrls() != null) {
            for (String url : systemConfig.getSecurityIgnoreUrls()) {
                if (!allIgnores.contains(url)) {
                    allIgnores.add(url);
                }
            }
        }
        String[] ignores = new String[allIgnores.size()];
        registry.addInterceptor(tokenHandlerInterceptor)
                .addPathPatterns("/api/wx/**", "/api/admin/**")
                .excludePathPatterns(allIgnores.toArray(ignores));
        super.addInterceptors(registry);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedMethods("*")
                .allowedOrigins("*")
                .allowedHeaders("*");
        super.addCorsMappings(registry);
    }

}
