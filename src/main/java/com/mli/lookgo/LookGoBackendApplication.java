package com.mli.lookgo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan({"com.mli.lookgo.core.dao", "com.mli.lookgo.module.*.dao"})
@EnableScheduling
public class LookGoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LookGoBackendApplication.class, args);
    }
}
