package com.ethan.janus.core.config;

import com.ethan.janus.core.rollback.JanusRollback;
import com.ethan.janus.core.rollback.JanusRollbackComponent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JanusConfig {

    @Bean
    @ConditionalOnMissingBean(value = JanusRollback.class)
    public JanusRollback janusRollback() {
        return new JanusRollbackComponent();
    }
}
