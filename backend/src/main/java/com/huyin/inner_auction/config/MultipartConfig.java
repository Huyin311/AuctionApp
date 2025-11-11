package com.huyin.inner_auction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // adjust limits as needed
        factory.setMaxFileSize(DataSize.ofMegabytes(10));    // max per file
        factory.setMaxRequestSize(DataSize.ofMegabytes(20)); // max per request (all files + form)
        return factory.createMultipartConfig();
    }
}