package com.ethan.janus.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;

@Configuration
public class JanusThreadPoolConfig {

    @Autowired
    private JanusThreadPoolComponent janusThreadPoolComponent;

    @Bean
    @ConditionalOnMissingBean(name = "janusBranchThreadPool")
    public ExecutorService janusBranchThreadPool() {
        return janusThreadPoolComponent.getJanusBranchThreadPool();
    }

    @Bean
    @ConditionalOnMissingBean(name = "janusCompareThreadPool")
    public ExecutorService janusCompareThreadPool() {
        return janusThreadPoolComponent.getJanusCompareThreadPool();
    }
}
