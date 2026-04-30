package com.eredar.janus.starter;

import com.eredar.janus.core.aspect.JanusAspect;
import com.eredar.janus.core.aspect.JanusPluginAspect;
import com.eredar.janus.core.compare.JanusCompare;
import com.eredar.janus.core.compare.JanusCompareDefaultImpl;
import com.eredar.janus.core.config.JanusChecker;
import com.eredar.janus.core.config.JanusConfigProperties;
import com.eredar.janus.core.config.JanusExpressionEvaluator;
import com.eredar.janus.core.flow.JanusFlow;
import com.eredar.janus.core.flow.impl.*;
import com.eredar.janus.core.lifecycle.*;
import com.eredar.janus.core.manager.JanusCompareManager;
import com.eredar.janus.core.manager.JanusFlowManager;
import com.eredar.janus.core.manager.JanusPluginManager;
import com.eredar.janus.core.plugin.JanusPlugin;
import com.eredar.janus.core.rollback.JanusRollback;
import com.eredar.janus.core.rollback.JanusRollbackDefault;
import com.eredar.janus.core.threadpool.JanusBranchThreadPoolDefaultProvider;
import com.eredar.janus.core.threadpool.JanusBranchThreadPoolMetricsProvider;
import com.eredar.janus.core.threadpool.JanusThreadPoolFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Janus Spring Boot Starter 自动装配入口。
 */
public class JanusAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "janus")
    public JanusConfigProperties janusConfigProperties() {
        return new JanusConfigProperties();
    }

    @Bean
    public JanusThreadPoolFactory janusThreadPoolFactory() {
        return new JanusThreadPoolFactory();
    }

    @Bean(name = "janusBranchThreadPool")
    @ConditionalOnMissingBean(name = "janusBranchThreadPool")
    public ExecutorService janusBranchThreadPool(JanusThreadPoolFactory janusThreadPoolFactory) {
        return janusThreadPoolFactory.getJanusBranchThreadPool();
    }

    @Bean(name = "janusCompareThreadPool")
    @ConditionalOnMissingBean(name = "janusCompareThreadPool")
    public ExecutorService janusCompareThreadPool(JanusThreadPoolFactory janusThreadPoolFactory) {
        return janusThreadPoolFactory.getJanusCompareThreadPool();
    }

    @Bean
    @ConditionalOnMissingBean(value = JanusRollback.class)
    public JanusRollback janusRollback() {
        return new JanusRollbackDefault();
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
    public LifecycleDecoratorManager lifecycleDecoratorManager(CoreLifecycle coreLifecycle,
                                                               TimeLifecycle timeLifecycle,
                                                               RollbackLifecycle rollbackLifecycle,
                                                               HigherPluginsExecuteLifecycle higherPluginsExecuteLifecycle,
                                                               LowerPluginsExecuteLifecycle lowerPluginsExecuteLifecycle) {
        return new LifecycleDecoratorManager(
                coreLifecycle,
                timeLifecycle,
                rollbackLifecycle,
                higherPluginsExecuteLifecycle,
                lowerPluginsExecuteLifecycle
        );
    }

    @Bean
    public JanusCompare defaultJanusCompare() {
        return new JanusCompareDefaultImpl();
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
    public JanusFlowManager janusFlowManager(List<JanusFlow> janusFlowList, JanusConfigProperties janusConfigProperties) {
        return new JanusFlowManager(janusFlowList, janusConfigProperties);
    }

    @Bean
    public JanusExpressionEvaluator janusExpressionEvaluator() {
        return new JanusExpressionEvaluator();
    }

    @Bean
    public JanusChecker janusChecker() {
        return new JanusChecker();
    }

    @Bean
    public JanusFlow asyncJanusFlow() {
        return new AsyncCompareJanusFlow();
    }

    @Bean
    public JanusFlow doNotJanusFlow() {
        return new DoNotCompareJanusFlow();
    }

    @Bean
    public JanusFlow syncJanusFlow() {
        return new SyncCompareJanusFlow();
    }

    @Bean
    public JanusFlow syncRollbackAllJanusFlow() {
        return new SyncRollbackAllCompareJanusFlow();
    }

    @Bean
    public JanusFlow syncRollbackOneJanusFlow() {
        return new SyncRollbackOneCompareJanusFlow();
    }
}
