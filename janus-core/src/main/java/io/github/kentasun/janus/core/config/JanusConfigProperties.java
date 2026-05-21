package io.github.kentasun.janus.core.config;

import io.github.kentasun.janus.core.constants.JanusCompareType;
import io.github.kentasun.janus.core.constants.JanusConstants;
import io.github.kentasun.janus.core.exception.JanusException;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JanusConfigProperties {

    // 是否校验：@Janus 注解中的 methodId 有无重复。 默认校验，有重复启动时会报错。
    private Boolean isMethodIdDuplicateCheck = Boolean.TRUE;

    // 总开关 Y-开启，N-关闭。不配置，默认开启
    private Boolean isOpen = Boolean.TRUE;

    /**
     * 未配置具体的分流开关时默认分支使用哪个。
     * <p>可以配置：</p>
     * <p>1. primary: 代表添加 Janus 注解的分支</p>
     * <p>2. secondary: 代表添加 Secondary 注解的 Service 所在分支</p>
     * <p>不配置该项时，默认为 secondary，因为一般 secondary 代表老分支，是正确的。</p>
     */
    private String defaultMasterBranch = JanusConstants.SECONDARY;

    /**
     * 默认比对类型，见 {@link io.github.kentasun.janus.core.constants.JanusCompareType}。
     * <p>默认值为 ASYNC_COMPARE 异步比对。</p>
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
     *
     * @return {@code true} -关闭；{@code false} -开启
     */
    public Boolean isClosed() {
        return !this.isOpen;
    }

    public Boolean getIsMethodIdDuplicateCheck() {
        return isMethodIdDuplicateCheck;
    }

    public void setIsMethodIdDuplicateCheck(Boolean methodIdDuplicateCheck) {
        isMethodIdDuplicateCheck = methodIdDuplicateCheck;
    }

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean open) {
        isOpen = open;
    }

    public String getDefaultMasterBranch() {
        return defaultMasterBranch;
    }

    public void setDefaultMasterBranch(String defaultMasterBranch) {
        this.defaultMasterBranch = defaultMasterBranch;
    }

    public String getDefaultCompareType() {
        return defaultCompareType;
    }

    public void setDefaultCompareType(String defaultCompareType) {
        this.defaultCompareType = defaultCompareType;
    }

    public ThreadPoolProperty getJanusBranchThreadPool() {
        return janusBranchThreadPool;
    }

    public void setJanusBranchThreadPool(ThreadPoolProperty janusBranchThreadPool) {
        this.janusBranchThreadPool = janusBranchThreadPool;
    }

    public ThreadPoolProperty getJanusCompareThreadPool() {
        return janusCompareThreadPool;
    }

    public void setJanusCompareThreadPool(ThreadPoolProperty janusCompareThreadPool) {
        this.janusCompareThreadPool = janusCompareThreadPool;
    }

    public AsyncCompareThrottling getAsyncCompareThrottling() {
        return asyncCompareThrottling;
    }

    public void setAsyncCompareThrottling(AsyncCompareThrottling asyncCompareThrottling) {
        this.asyncCompareThrottling = asyncCompareThrottling;
    }

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
                throw new JanusException(String.format("TimeUnit[%s] not found", this.unit));
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
                throw new JanusException(String.format("RejectedExecutionHandler[%s] not found", this.rejectedHandler));
            }
            return null;
        }

        public Integer getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(Integer corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public Integer getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(Integer maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public Long getKeepAliveTime() {
            return keepAliveTime;
        }

        public void setKeepAliveTime(Long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Integer getWorkQueueSize() {
            return workQueueSize;
        }

        public void setWorkQueueSize(Integer workQueueSize) {
            this.workQueueSize = workQueueSize;
        }

        public void setRejectedHandler(String rejectedHandler) {
            this.rejectedHandler = rejectedHandler;
        }
    }

    // 异步比对限流 相关配置项
    public static class AsyncCompareThrottling {
        // 异步比对限流是否开启。默认开启
        private Boolean isOpen = Boolean.TRUE;
        // 异步比对限流比例。默认 0.8，即 janusBranchThreadPool 的队列占用率到80%，就开始限制流量超过平均值的方法的流量
        private Double limitRatio = 0.8;

        public boolean isClosed() {
            return !this.isOpen;
        }

        public Boolean getIsOpen() {
            return isOpen;
        }

        public void setIsOpen(Boolean open) {
            isOpen = open;
        }

        public Double getLimitRatio() {
            return limitRatio;
        }

        public void setLimitRatio(Double limitRatio) {
            this.limitRatio = limitRatio;
        }
    }
}
