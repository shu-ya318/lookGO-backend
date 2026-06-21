package com.mli.lookgo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mli.lookgo.module.*.dao")
public class LookGoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LookGoBackendApplication.class, args);
    }
}
