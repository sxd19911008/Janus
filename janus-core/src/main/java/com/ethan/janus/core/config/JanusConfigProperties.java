package com.ethan.janus.core.config;

import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.core.constants.JanusConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@ConfigurationProperties(prefix = "janus")
@Validated
@Data
public class JanusConfigProperties {

    // 总开关 Y-开启，N-关闭。不配置，默认开启
    private Boolean isOpen = Boolean.TRUE;

    /*
     * 未配置具体的分流开关时默认分支使用哪个。
     * 可以配置：
     *      1.primary: 代表添加 Janus 注解的分支
     *      2.secondary: 代表添加 Secondary 注解的 Service 所在分支
     * 不配置该项时，默认为 secondary，因为一般 secondary 代表老分支，是正确的。
     */
    private String masterBranch = JanusConstants.SECONDARY;

    /*
     * 未配置具体的比对开关时，默认是否比对。
     * 可配置项：
     *      1.true: 开启比对功能
     *      2.false: 关闭比对功能
     * 不配置该项时，默认为 true，表示如果不配置具体的比对开关的情况下，默认开启比对功能。
     */
    private Boolean isDefaultCompare = true;

    /*
     * 默认比对类型，见 CompareType
     * 默认值为 ASYNC_COMPARE 异步比对
     */
    private String defaultCompareType = CompareType.ASYNC_COMPARE.name();

    // 异步执行比对分支的线程池配置
    private ThreadPoolProperty janusBranchThreadPool;

    // 异步比对的线程池配置
    private ThreadPoolProperty janusCompareThreadPool;

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
}
