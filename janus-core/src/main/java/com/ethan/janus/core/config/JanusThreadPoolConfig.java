package com.ethan.janus.core.config;

import com.ethan.janus.core.utils.JanusUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class JanusThreadPoolConfig {

    @Autowired
    private JanusConfig janusConfig;

    @Bean
    @ConditionalOnMissingBean(name = "janusThreadPool")
    public ExecutorService janusThreadPool() {
        return this.getExecutorService();
    }

    /**
     * 创建原始线程池
     */
    public ExecutorService getExecutorService() {
        /* 线程工厂 */
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("pool-janusThreadPool-thread-");
        // 设为非守护线程，主线程完成后子线程会继续完成自己的任务，而不会因为主线程结束而中断。
        threadFactory.setDaemon(false);
        // 线程会继承父线程的优先级。为了防止线程池里的线程因为“出身”问题导致执行过慢或抢占过多资源，显式重置为“正常优先级”
        threadFactory.setThreadPriority(Thread.NORM_PRIORITY);

        /* 线程池配置项 */
        JanusConfig.ThreadPoolProperty properties = Optional.ofNullable(janusConfig.getThreadPool()).orElse(new JanusConfig.ThreadPoolProperty());

        /* 创建线程池 */
        return new ThreadPoolExecutor(
                JanusUtils.defaultIfNull(properties.getCorePoolSize(), 5),
                JanusUtils.defaultIfNull(properties.getMaximumPoolSize(), 10),
                JanusUtils.defaultIfNull(properties.getKeepAliveTime(), 1800L),
                JanusUtils.defaultIfNull(properties.getUnit(), TimeUnit.SECONDS),
                new LinkedBlockingDeque<>(JanusUtils.defaultIfNull(properties.getWorkQueueSize(), 50000)),
                threadFactory,
                JanusUtils.defaultIfNull(properties.getRejectedHandler(), new ThreadPoolExecutor.DiscardOldestPolicy())
        );
    }
}
