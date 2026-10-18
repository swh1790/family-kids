package com.family.kids;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.family.kids.mapper")
public class KidsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KidsApplication.class, args);
    }
}
