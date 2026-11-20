package com.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 配置：将上架照片目录以 /uploads/** 静态暴露，供前端展示现场照片。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 照片存储目录（配置项 library.upload-dir，相对运行目录） */
    @Value("${library.upload-dir:uploads/shelving/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        // location 必须以 / 结尾，否则 Spring 会把最后一段当作文件而非目录
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
