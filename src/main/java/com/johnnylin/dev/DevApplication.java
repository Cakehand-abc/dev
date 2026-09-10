package com.johnnylin.dev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智慧养老健康管理系统后端启动引导类。
 *
 * <p>基于 Spring Boot 3 构建，整合 MyBatis-Plus、Spring Security 与 Web 服务，
 * 提供长者健康监护、穿戴设备 IoT 数据接入、地理围栏告警及统计分析等核心功能。
 */
@SpringBootApplication
public class DevApplication {

    /**
     * 应用程序入口主方法。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DevApplication.class, args);
    }

}
