package com.qrcode.attendance.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class QrCodeConfig {

    @Value("${app.qr-code.base-url}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        log.info("✅ 二维码基础URL（配置文件）: {}", baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}