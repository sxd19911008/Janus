package com.ethan.janus.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单测专用启动类，用于触发完整 Spring Boot 自动装配
 */
@SpringBootApplication
@MapperScan("com.ethan.janus.starter.dao")
public class JanusTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(JanusTestApplication.class, args);
    }
}