package com.johnnylin.dev.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.*;
import java.time.*;
import java.io.IOException;

/**
 * Jackson JSON 序列化与反序列化定制配置类。
 *
 * <p>主要解决两类前后端交互的常见问题：
 * <ul>
 *   <li>将 64 位 Long 型整数序列化为 String，防止前端 JavaScript (IEEE 754 53 位限制) 精度丢失</li>
 *   <li>将 LocalDateTime 自动附带 +08:00 时区偏移并输出标准 ISO-8601 字符串</li>
 * </ul>
 */
@Configuration
public class JsonConfig {

    /**
     * 自定义 Jackson ObjectMapper 构建器。
     *
     * @return 用于配置 Jackson 序列化规则的自定义构建器实例
     */
    @Bean
    Jackson2ObjectMapperBuilderCustomizer json() {
        return b -> {
            // 将 Long 型转为 String 序列化输出，防御前端大数精度丢失
            b.serializerByType(Long.class, ToStringSerializer.instance);
            // 规范化输出带 +08:00 时区的 ISO-8601 时间格式
            b.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator g, SerializerProvider p) throws IOException {
                    g.writeString(value.atOffset(ZoneOffset.ofHours(8)).toString());
                }
            });
        };
    }
}
