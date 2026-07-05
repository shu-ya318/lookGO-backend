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
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.serializerByType(LocalDate.class,
                    new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            // 列舉欄位若收到數字，預設會被解讀成「ordinal（宣告順序索引）」而非業務自訂的 code，
            // 容易誤判（例如 ChatTypeEnum.TEXT 的 code=1 但 ordinal=0）。開啟後改成直接拒絕數字輸入，
            // 強制前端一律以列舉常數名稱字串（如 "TEXT"）傳遞，避免靜默誤判。
            builder.featuresToEnable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
        };
    }
}