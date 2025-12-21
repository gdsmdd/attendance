package com.qrcode.attendance; // 请替换为您的实际包名

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class AttendanceApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		// 关键：配置应用入口，供外置Servlet容器调用
		return application.sources(AttendanceApplication.class);
	}

	public static void main(String[] args) {
		// 保留此方法，仍可用于 IDE 中直接运行或打包为可执行JAR
		SpringApplication.run(AttendanceApplication.class, args);
	}
}