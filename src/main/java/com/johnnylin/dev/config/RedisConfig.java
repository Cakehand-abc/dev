package com.johnnylin.dev.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 基础配置类。
 *
 * <p>定制 {@link RedisTemplate} 键值序列化机制：
 * <ul>
 *   <li>Key 与 HashKey 采用 {@link StringRedisSerializer}，确保键名纯文本可视化</li>
 *   <li>Value 与 HashValue 采用 {@link GenericJackson2JsonRedisSerializer}，支持复杂 JSON 对象结构</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    /**
     * 构建并配置通用的 RedisTemplate 实例。
     *
     * @param connectionFactory Redis 连接工厂
     * @param objectMapper Spring 上下文中配置的 ObjectMapper 实例
     * @return 定制化序列化策略的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Key 与 HashKey 序列化为纯文本 String
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 与 HashValue 序列化为标准 JSON
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
