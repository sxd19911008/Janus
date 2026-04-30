package com.eredar.janus.core.config;

import com.eredar.janus.core.constants.JanusCompareType;
import com.eredar.janus.core.constants.JanusConstants;
import lombok.Data;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Data
public class JanusConfigProperties {

    // 是否校验：@Janus 注解中的 methodId 有无重复。 默认校验，有重复启动时会报错。
    private Boolean isMethodIdDuplicateCheck = Boolean.TRUE;

    // 总开关 Y-开启，N-关闭。不配置，默认开启
    private Boolean isOpen = Boolean.TRUE;

    /*
     * 未配置具体的分流开关时默认分支使用哪个。
     * 可以配置：
     *      1.primary: 代表添加 Janus 注解的分支
     *      2.secondary: 代表添加 Secondary 注解的 Service 所在分支
     * 不配置该项时，默认为 secondary，因为一般 secondary 代表老分支，是正确的。
     */
    private String defaultMasterBranch = JanusConstants.SECONDARY;

    /*
     * 默认比对类型，见 JanusCompareType
     * 默认值为 ASYNC_COMPARE 异步比对
     */
    private String defaultCompareType = JanusCompareType.ASYNC_COMPARE;

    // 异步执行比对分支的线程池配置
    private ThreadPoolProperty janusBranchThreadPool;

    // 异步比对的线程池配置
    private ThreadPoolProperty janusCompareThreadPool;

    // 异步比对限流 相关配置项
    private AsyncCompareThrottling asyncCompareThrottling = new AsyncCompareThrottling();

    /**
     * 总开关是否关闭
     */
    public Boolean isClosed() {
        return !this.isOpen;
    }

    @Data
    public static class ThreadPoolProperty {

        private Integer corePoolSize;
        private Integer maximumPoolSize;
        private Long keepAliveTime;
        private String unit;
        private Integer workQueueSize;
        private String rejectedHandler;

        public TimeUnit getUnit() {
            if (this.unit != null) {
                switch (this.unit) {
                    case "NANOSECONDS":
                        return TimeUnit.NANOSECONDS;
                    case "MICROSECONDS":
                        return TimeUnit.MICROSECONDS;
                    case "MILLISECONDS":
                        return TimeUnit.MILLISECONDS;
                    case "SECONDS":
                        return TimeUnit.SECONDS;
                    case "MINUTES":
                        return TimeUnit.MINUTES;
                    case "HOURS":
                        return TimeUnit.HOURS;
                    case "DAYS":
                        return TimeUnit.DAYS;
                }
            }
            return null;
        }

        public RejectedExecutionHandler getRejectedHandler() {
            if (this.rejectedHandler != null) {
                switch (this.rejectedHandler) {
                    case "CallerRunsPolicy":
                        return new ThreadPoolExecutor.CallerRunsPolicy();
                    case "AbortPolicy":
                        return new ThreadPoolExecutor.AbortPolicy();
                    case "DiscardPolicy":
                        return new ThreadPoolExecutor.DiscardPolicy();
                    case "DiscardOldestPolicy":
                        return new ThreadPoolExecutor.DiscardOldestPolicy();
                }
            }
            return null;
        }
    }

    // 异步比对限流 相关配置项
    @Data
    public static class AsyncCompareThrottling {
        // 异步比对限流是否开启。默认开启
        private Boolean isOpen = Boolean.TRUE;
        // 异步比对限流比例。默认 0.8，即 janusBranchThreadPool 的队列占用率到80%，就开始限制流量超过平均值的方法的流量
        private Double limitRatio = 0.8;

        public boolean isClosed() {
            return !this.isOpen;
        }
    }
}
