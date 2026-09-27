package com.finance.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 静态资源配置
 * 将项目根目录下的 uploads 文件夹映射为静态资源，
 * 前端可通过 /uploads/** 路径直接访问上传的文件（如头像图片）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 URL 路径 /uploads/** 映射到本地文件系统上的 uploads/ 目录
        // file:uploads/ 是相对于项目运行目录的路径，即项目根目录下的 uploads 文件夹
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
