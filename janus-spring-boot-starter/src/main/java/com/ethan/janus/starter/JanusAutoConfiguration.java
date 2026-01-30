package com.ethan.janus.starter;

import com.ethan.janus.core.lifecycle.JanusAspect;
import com.ethan.janus.core.config.JanusConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Janus Spring Boot Starter 自动装配入口。
 * <p>
 * 该配置类负责：
 * 1. 在类路径存在 Janus 相关类时触发自动装配；
 * 2. 显式导入 Janus 的核心 Bean（配置、线程池、切面）；
 * 3. 避免业务方手写 @ComponentScan 或 @Import。
 * <p>
 * 这样业务项目只需引入 Starter 依赖，即可完成 Janus 能力的装配。
 */
@Configuration
@ConditionalOnClass(JanusAspect.class)
@ComponentScan(basePackages = "com.ethan.janus") // 扫描 Janus 核心包，自动注册组件
@EnableConfigurationProperties(JanusConfig.class) // 启用 Janus 配置属性绑定
public class JanusAutoConfiguration {
    // 该类仅用于触发自动装配，不需要额外代码实现。
    // 保留此空类结构是为了让 Spring Boot 自动装配机制可识别入口。
}
