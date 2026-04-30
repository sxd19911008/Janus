package com.eredar.janus.core.flow.impl;

import com.eredar.janus.core.config.JanusConfigProperties;
import com.eredar.janus.core.constants.JanusCompareType;
import com.eredar.janus.core.dto.JanusContextImpl;
import com.eredar.janus.core.flow.JanusFlow;
import com.eredar.janus.core.threadpool.JanusBranchThreadPoolMetricsProvider;
import com.eredar.janus.core.utils.JanusLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link JanusCompareType#ASYNC_COMPARE} 流程实现
 * <p>在调用方线程中执行主分支，然后将比对分支的执行和结果比对提交到异步线程池中处理，
 * 主分支的返回对用户完全无感知延迟。内置自适应限流逻辑，当线程池压力过大时会丢弃积压严重的方法的比对任务。</p>
 */
@Slf4j
public class AsyncCompareJanusFlow implements JanusFlow {

    // 用于异步执行比对分支的线程池
    @Qualifier("janusBranchThreadPool")
    @Autowired
    private ExecutorService janusBranchThreadPool;

    @Autowired
    private JanusConfigProperties janusConfigProperties;

    @Autowired
    private JanusBranchThreadPoolMetricsProvider janusBranchThreadPoolMetricsProvider;

    // 统计当前时间点，每个异步执行compareBranch的方法的流量。每次进入本切面，数字加1；异步比对完成后，数字减1。
    private final Map<Method, AtomicInteger> methodCountMap = new ConcurrentHashMap<>();

    @Override
    public String getCompareType() {
        return JanusCompareType.ASYNC_COMPARE;
    }

    @Override
    public void execute(JanusContextImpl context) {
        // 计数器加 1，统计该方法当前待处理的异步比对任务数
        this.increment(context);
        try {
            // 异步执行比对分支，需要设置相关标识
            context.getCompareBranch().setIsAsync(true);

            // 执行主分支（对用户返回结果起决定性作用）
            context.masterBranchExecute();

            // 插件可能在主分支执行期间动态关闭比对，需再次检查
            if (context.doNotCompare()) {
                // 无需异步任务，计数器归还
                this.decrement(context);
                return;
            }

            // 线程池压力较大且当前方法积压严重时，丢弃本次比对任务以保护线程池
            if (this.shouldThrottle(context)) {
                this.decrement(context);
                return;
            }

            // 提交异步任务：执行比对分支 → 比对结果 → 计数器归还
            janusBranchThreadPool.execute(() -> {
                try {
                    context.compareBranchExecute();
                    context.compare();
                } finally {
                    // 无论成功与否，任务结束后必须归还计数，防止计数泄漏
                    this.decrement(context);
                }
            });
        } catch (Throwable e) {
            // 提交异步任务失败（如线程池拒绝），计数器归还，异常不向上传播
            this.decrement(context);
            log.error(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:AsyncCompareJanusFlow] >> exception=",
                    JanusLogUtils.FAIL_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    e
            );
        }
    }

    /**
     * 流量计数器加 1
     * <p>仅当异步比对开启且需要比对时，才操作计数器。</p>
     *
     * @param context 上下文
     */
    private void increment(JanusContextImpl context) {
        try {
            if (janusConfigProperties.getAsyncCompareThrottling().isClosed()) {
                return;
            }
            if (context.needCompare()) {
                AtomicInteger counter = methodCountMap.computeIfAbsent(context.getMethod(), k -> new AtomicInteger(0));
                counter.incrementAndGet();
            }
        } catch (Throwable e) {
            log.error(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:throttle#increment] >> exception=",
                    JanusLogUtils.FAIL_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    e
            );
        }
    }

    /**
     * 流量计数器减 1
     *
     * @param context 上下文
     */
    private void decrement(JanusContextImpl context) {
        try {
            if (janusConfigProperties.getAsyncCompareThrottling().isClosed()) {
                return;
            }
            AtomicInteger counter = methodCountMap.get(context.getMethod());
            if (counter != null) {
                counter.decrementAndGet();
            }
        } catch (Throwable e) {
            log.error(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:throttle#decrement] >> exception=",
                    JanusLogUtils.FAIL_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    e
            );
        }
    }

    /**
     * 判断当前方法是否应该被限流
     * <p>只有在线程池处于高压状态，且当前方法的待处理任务数达到或超过所有方法的平均值时，才触发限流。</p>
     *
     * @param context 上下文
     * @return true-触发限流，应丢弃本次比对任务；false-正常执行
     */
    private boolean shouldThrottle(JanusContextImpl context) {
        // 校验开关
        if (janusConfigProperties.getAsyncCompareThrottling().isClosed()) {
            return false;
        }

        // 只有线程池有压力时才进行限流判定，保证性能且不干扰正常扩容
        if (!this.isHighPressure()) {
            return false;
        }

        // 当前方法的流量
        int currentMethodCount;
        AtomicInteger atomicInteger = methodCountMap.get(context.getMethod());
        if (atomicInteger == null) {
            currentMethodCount = 0;
        } else {
            currentMethodCount = atomicInteger.get();
        }

        // 流量太少，不需要做复杂判断，直接进行比对
        if (currentMethodCount < 10) {
            return false;
        }

        Collection<AtomicInteger> values = methodCountMap.values();
        int totalCount = values.stream().mapToInt(AtomicInteger::get).sum(); // 当前总数量
        int activeMethodsNum = values.size(); // 当前切面处理过的异步执行compareBranch的总方法数
        int average = totalCount / activeMethodsNum; // 每个方法的平均流量

        /*
         * 如果当前方法并发数等于或超过平均值，则触发限流。
         * 必须是大于等于，缺少等于会导致以下场景判断失效：
         * 1. 只有一个接口的场景。
         * 2. 所有接口的流量都完全一样的极端场景。
         */
        return currentMethodCount >= average;
    }

    /**
     * 判断执行比对分支的线程池是否处于高压状态
     */
    private boolean isHighPressure() {
        // 队列当前 size
        int currentSize = janusBranchThreadPoolMetricsProvider.getQueueSize(janusBranchThreadPool);
        // 队列总 size
        int capacity = janusBranchThreadPoolMetricsProvider.getQueueCapacity(janusBranchThreadPool);
        // 占用比例
        double usageRatio = (double) currentSize / capacity;
        // 阈值默认 0.8
        return usageRatio > janusConfigProperties.getAsyncCompareThrottling().getLimitRatio();
    }
}
