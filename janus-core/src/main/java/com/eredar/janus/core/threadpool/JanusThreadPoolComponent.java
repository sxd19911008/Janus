package com.eredar.janus.core.threadpool;

import com.eredar.janus.core.config.JanusConfigProperties;
import com.eredar.janus.core.utils.JanusUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JanusThreadPoolComponent {

    @Autowired
    private JanusConfigProperties janusConfigProperties;

    /**
     * 创建执行比对分支的线程池
     */
    public ExecutorService getJanusBranchThreadPool() {
        /* 线程工厂 */
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("pool-janusBranchThreadPool-thread-");
        // 设为非守护线程，主线程完成后子线程会继续完成自己的任务，而不会因为主线程结束而中断。
        threadFactory.setDaemon(false);
        // 线程会继承父线程的优先级。为了防止线程池里的线程因为“出身”问题导致执行过慢或抢占过多资源，显式重置为“正常优先级”
        threadFactory.setThreadPriority(Thread.NORM_PRIORITY);

        /* 线程池配置项 */
        JanusConfigProperties.ThreadPoolProperty properties = Optional.ofNullable(janusConfigProperties.getJanusBranchThreadPool()).orElse(new JanusConfigProperties.ThreadPoolProperty());

        /* 创建线程池 */
        return new ThreadPoolExecutor(
                JanusUtils.defaultIfNull(properties.getCorePoolSize(), 16),
                JanusUtils.defaultIfNull(properties.getMaximumPoolSize(), 16),
                JanusUtils.defaultIfNull(properties.getKeepAliveTime(), 0L),
                JanusUtils.defaultIfNull(properties.getUnit(), TimeUnit.SECONDS),
                new LinkedBlockingDeque<>(JanusUtils.defaultIfNull(properties.getWorkQueueSize(), 10000)),
                threadFactory,
                JanusUtils.defaultIfNull(properties.getRejectedHandler(), new ThreadPoolExecutor.DiscardOldestPolicy())
        );
    }

    /**
     * 创建比对线程池
     */
    public ExecutorService getJanusCompareThreadPool() {
        /* 线程工厂 */
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("pool-janusCompareThreadPool-thread-");
        // 设为非守护线程，主线程完成后子线程会继续完成自己的任务，而不会因为主线程结束而中断。
        threadFactory.setDaemon(false);
        // 线程会继承父线程的优先级。为了防止线程池里的线程因为“出身”问题导致执行过慢或抢占过多资源，显式重置为“正常优先级”
        threadFactory.setThreadPriority(Thread.NORM_PRIORITY);

        /* 线程池配置项 */
        JanusConfigProperties.ThreadPoolProperty properties = Optional.ofNullable(janusConfigProperties.getJanusCompareThreadPool()).orElse(new JanusConfigProperties.ThreadPoolProperty());

        /* 创建线程池 */
        return new ThreadPoolExecutor(
                JanusUtils.defaultIfNull(properties.getCorePoolSize(), 3),
                JanusUtils.defaultIfNull(properties.getMaximumPoolSize(), 5),
                JanusUtils.defaultIfNull(properties.getKeepAliveTime(), 600L),
                JanusUtils.defaultIfNull(properties.getUnit(), TimeUnit.SECONDS),
                new LinkedBlockingDeque<>(JanusUtils.defaultIfNull(properties.getWorkQueueSize(), 50000)),
                threadFactory,
                JanusUtils.defaultIfNull(properties.getRejectedHandler(), new ThreadPoolExecutor.DiscardOldestPolicy())
        );
    }
}
