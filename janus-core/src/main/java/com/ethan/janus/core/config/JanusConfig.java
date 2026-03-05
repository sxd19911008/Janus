package com.ethan.janus.core.config;

import com.ethan.janus.core.compare.JanusCompare;
import com.ethan.janus.core.compare.JanusCompareDefaultImpl;
import com.ethan.janus.core.rollback.JanusRollback;
import com.ethan.janus.core.rollback.JanusRollbackDefault;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JanusConfig {

    @Bean
    @ConditionalOnMissingBean(value = JanusRollback.class)
    public JanusRollback janusRollback() {
        return new JanusRollbackDefault();
    }

    @Bean
    @ConditionalOnMissingBean(value = JanusCompare.class)
    public JanusCompare defaultJanusCompare() {
        return new JanusCompareDefaultImpl();
    }
}
