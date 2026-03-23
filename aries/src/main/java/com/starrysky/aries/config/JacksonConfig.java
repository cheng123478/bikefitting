package com.starrysky.aries.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * JacksonConfig
 * 解决返回客户端日期格式问题
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 17:21:38
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义LocalDate反序列化器，支持日期和日期时间格式
     * 可以接受 "yyyy-MM-dd" 或 "yyyy-MM-dd HH:mm:ss" 格式
     */
    private static class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateString = p.getText().trim();
            if (!StringUtils.hasText(dateString)) {
                return null;
            }

            // 先尝试解析日期时间格式
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateString, DATETIME_FORMATTER);
                return dateTime.toLocalDate();
            } catch (DateTimeParseException e) {
                // 如果失败，尝试解析日期格式
                try {
                    return LocalDate.parse(dateString, DATE_FORMATTER);
                } catch (DateTimeParseException e2) {
                    throw new IOException("无法解析日期格式: " + dateString + "，支持的格式: yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss", e2);
                }
            }
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder ->
                builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        // long类型转string， 前端处理Long类型，数值过大会丢失精度
                        .serializerByType(Long.class, ToStringSerializer.instance)
                        .serializerByType(Long.TYPE, ToStringSerializer.instance)
                        .serializationInclusion(JsonInclude.Include.ALWAYS)
                        //指定反序列化类型，也可以使用@JsonFormat(pattern = "yyyy-MM-dd")替代。主要是mvc接收日期时使用
                        .deserializerByType(LocalTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .deserializerByType(LocalDate.class, new FlexibleLocalDateDeserializer())
                        .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        // 日期序列化，主要返回数据时使用
                        .serializerByType(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")))
                        .serializerByType(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}
