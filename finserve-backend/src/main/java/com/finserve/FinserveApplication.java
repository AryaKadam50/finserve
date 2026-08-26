package com.finserve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FinserveApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinserveApplication.class, args);
    }
}