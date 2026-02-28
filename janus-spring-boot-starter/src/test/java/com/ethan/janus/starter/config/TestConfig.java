package com.ethan.janus.starter.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration
public class TestConfig {
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                /*
                 * DB_CLOSE_DELAY=-1   保持数据库连接，JVM 存活期间不关闭
                 * DB_CLOSE_ON_EXIT=FALSE   JVM 退出时不自动关闭数据库
                 * MODE=MySQL   兼容 MySQL 语法（如 AUTO_INCREMENT）
                 * INIT=CREATE SCHEMA IF NOT EXISTS PUBLIC   启动时创建 PUBLIC schema（若不存在）
                 */
                .url("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;INIT=CREATE SCHEMA IF NOT EXISTS PUBLIC")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();
    }
}
