package com.qrcode.attendance.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.net.InetAddress;
@Configuration
@Slf4j
public class QrCodeConfig {

    private String baseUrl;

    @PostConstruct
    public void init() {
        try {
            // 获取本机IP地址
            String ipAddress = InetAddress.getLocalHost().getHostAddress();

            // 构建baseUrl
            baseUrl = "http://" + ipAddress + ":8080/attendance";

            log.info("✅ 二维码基础URL（动态生成）: {}", baseUrl);
        } catch (Exception e) {
            // 失败时使用默认值
            baseUrl = "http://localhost:8080/attendance";
            log.error("获取服务器IP失败，使用默认URL: {}", baseUrl, e);
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}