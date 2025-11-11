package com.huyin.inner_auction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files from ./uploads mapped to /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // dev only: allow all origins for api and uploads; tighten in production
        registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("*");
        registry.addMapping("/uploads/**").allowedOrigins("*").allowedMethods("GET");
    }
}