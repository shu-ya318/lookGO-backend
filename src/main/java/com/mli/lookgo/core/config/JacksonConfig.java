package com.mli.lookgo.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 處理 Jackson 的客製化配置。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Configuration
public class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // 避免 LocalDate、LocalDateTime 被轉為時間戳
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // LocalDate 輸出格式固定為 yyyy-MM-dd
            builder.serializerByType(LocalDate.class,
                    new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            // 1. 效果：反序列化時，列舉欄位若收到數字直接拋出例外，強制前端以名稱字串（如 "TEXT"）傳遞
            // 2. 避免的錯誤：數字被靜默解讀為 ordinal (enum 的預設索引)，而非業務自訂的 code，造成誤判（如 TEXT 的 code=1 但 ordinal=0）
            builder.featuresToEnable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
        };
    }
}