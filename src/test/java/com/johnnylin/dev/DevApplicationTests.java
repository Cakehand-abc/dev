package com.johnnylin.dev;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/**
 * Spring Boot 应用程序上下文基础加载测试。
 *
 * <p>验证应用上下文环境在注入所有 Bean 配置、数据源连接与安全过滤器链时能够正常无异常初始化。
 */
@SpringBootTest
@AutoConfigureMockMvc
class DevApplicationTests {

    /**
     * 验证 Spring 核心容器与各层组件上下文能否成功装载。
     */
    @Test
    void contextLoads() {
    }

}

