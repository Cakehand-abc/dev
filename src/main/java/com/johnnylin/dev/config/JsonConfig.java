package com.johnnylin.dev.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.*;
import java.time.*;
import java.io.IOException;

@Configuration
public class JsonConfig {
    @Bean Jackson2ObjectMapperBuilderCustomizer json() {
        return b -> {
            b.serializerByType(Long.class,ToStringSerializer.instance);
            b.serializerByType(LocalDateTime.class,new JsonSerializer<LocalDateTime>() {
                @Override public void serialize(LocalDateTime value,JsonGenerator g,SerializerProvider p)throws IOException {
                    g.writeString(value.atOffset(ZoneOffset.ofHours(8)).toString());
                }
            });
        };
    }
}
