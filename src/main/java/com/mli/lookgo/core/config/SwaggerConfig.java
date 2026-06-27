package com.mli.lookgo.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 處理 Swagger / OpenAPI 的客製化配置，包含 API 基本資訊與 JWT 安全驗證方案。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Configuration
public class SwaggerConfig {

        private static final String JWT_SCHEME = "Bearer Auth";

        /**
         * 建立並設定 OpenAPI 文件物件，定義 API 標題、版本與 JWT 安全驗證方案。
         *
         * @return OpenAPI
         */
        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .info(new Info().title("LookGo API").version("1.0.0"))
                                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME))
                                .components(new Components()
                                                .addSecuritySchemes(JWT_SCHEME,
                                                                new SecurityScheme()
                                                                                .name(JWT_SCHEME)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")));
        }
}
