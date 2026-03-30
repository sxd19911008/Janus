package com.eredar.janus.starter;

import com.eredar.janus.core.aspect.JanusAspect;
import com.eredar.janus.core.aspect.JanusPluginAspect;
import com.eredar.janus.core.compare.JanusCompare;
import com.eredar.janus.core.compare.JanusCompareDefaultImpl;
import com.eredar.janus.core.config.JanusChecker;
import com.eredar.janus.core.config.JanusConfigProperties;
import com.eredar.janus.core.config.JanusExpressionEvaluator;
import com.eredar.janus.core.lifecycle.*;
import com.eredar.janus.core.manager.JanusCompareManager;
import com.eredar.janus.core.manager.JanusPluginManager;
import com.eredar.janus.core.plugin.JanusPlugin;
import com.eredar.janus.core.rollback.JanusRollback;
import com.eredar.janus.core.rollback.JanusRollbackDefault;
import com.eredar.janus.core.threadpool.JanusBranchThreadPoolDefaultProvider;
import com.eredar.janus.core.threadpool.JanusBranchThreadPoolMetricsProvider;
import com.eredar.janus.core.threadpool.JanusThreadPoolComponent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Janus Spring Boot Starter 自动装配入口。
 */
public class JanusAutoConfiguration {

    /**
     * 第一步：接收配置并组装 Core 包的对象
     * * Spring 会拦截这个 Bean 的创建，读取 application.yml 中以 "janus" 开头的配置，
     * 并通过反射调用 JanusConfigProperties 的 setter 方法完成组装。
     */
    @Bean
    @ConfigurationProperties(prefix = "janus")
    public JanusConfigProperties janusConfigProperties() {
        // 创建 Core 包的 DTO 对象
        return new JanusConfigProperties();
    }

    @Bean
    public JanusThreadPoolComponent janusThreadPoolComponent() {
        return new JanusThreadPoolComponent();
    }

    @Bean(name = "janusBranchThreadPool")
    @ConditionalOnMissingBean(name = "janusBranchThreadPool")
    public ExecutorService janusBranchThreadPool(JanusThreadPoolComponent janusThreadPoolComponent) {
        return janusThreadPoolComponent.getJanusBranchThreadPool();
    }

    @Bean(name = "janusCompareThreadPool")
    @ConditionalOnMissingBean(name = "janusCompareThreadPool")
    public ExecutorService janusCompareThreadPool(JanusThreadPoolComponent janusThreadPoolComponent) {
        return janusThreadPoolComponent.getJanusCompareThreadPool();
    }

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

    @Bean
    @ConditionalOnMissingBean(value = JanusBranchThreadPoolMetricsProvider.class)
    public JanusBranchThreadPoolMetricsProvider janusBranchThreadPoolMetricsProvider() {
        return new JanusBranchThreadPoolDefaultProvider();
    }

    @Bean
    public JanusAspect janusAspect() {
        return new JanusAspect();
    }

    @Bean
    public JanusPluginAspect janusPluginAspect() {
        return new JanusPluginAspect();
    }

    @Bean
    public CoreLifecycle coreLifecycle() {
        return new CoreLifecycle();
    }

    @Bean
    public HigherPluginsExecuteLifecycle higherPluginsExecuteLifecycle() {
        return new HigherPluginsExecuteLifecycle();
    }

    @Bean
    public LowerPluginsExecuteLifecycle lowerPluginsExecuteLifecycle() {
        return new LowerPluginsExecuteLifecycle();
    }

    @Bean
    public RollbackLifecycle rollbackLifecycle() {
        return new RollbackLifecycle();
    }

    @Bean
    public TimeLifecycle timeLifecycle() {
        return new TimeLifecycle();
    }

    @Bean
    public LifecycleDecoratorManager lifecycleDecoratorManager() {
        return new LifecycleDecoratorManager();
    }

    @Bean
    public JanusCompareManager janusCompareManager(List<JanusCompare> compareList) {
        return new JanusCompareManager(compareList);
    }

    @Bean
    public JanusPluginManager janusPluginManager(List<JanusPlugin> janusPluginList) {
        return new JanusPluginManager(janusPluginList);
    }

    @Bean
    public JanusExpressionEvaluator janusExpressionEvaluator() {
        return new JanusExpressionEvaluator();
    }

    @Bean
    public JanusChecker janusChecker() {
        return new JanusChecker();
    }
}
