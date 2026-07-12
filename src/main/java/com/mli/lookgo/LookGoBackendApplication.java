package com.mli.lookgo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/* 
 * Spring Boot 應用程式啟動類別，作為入口點。
 * 啟動時自動載入配置檔，並掃描指定的 Bean。(實現 DI: 呼叫每個 @Bean 方法取得物件實例，存入 Application Context，之後需要的地方直接由 Spring 容器注入依賴。)
 * 另外也有啟用排程功能。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@SpringBootApplication
@MapperScan({ "com.mli.lookgo.core.dao", "com.mli.lookgo.module.*.dao" })
@EnableScheduling
public class LookGoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LookGoBackendApplication.class, args);
    }
}
