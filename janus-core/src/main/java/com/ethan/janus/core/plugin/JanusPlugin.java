package com.ethan.janus.core.plugin;

import com.ethan.janus.core.annotation.Janus;
import com.ethan.janus.core.dto.JanusContext;

/**
 * Janus 插件接口
 * <p>1. 责任链模式，根据优先级决定插件进入和退出的时间。
 * <p>2. 优先级越高，进入越早，结束越晚。
 * <p>3. 特殊插件【落表比对回滚插件】，可以被设置优先级为0。其他插件优先级不可被设置为0。
 */
public interface JanusPlugin {

    /**
     * 最高优先级
     * @see java.lang.Integer#MIN_VALUE
     */
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /**
     * 最低优先级
     * @see java.lang.Integer#MAX_VALUE
     */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * 插件优先级。
     * <p>1. 默认最高优先级，即最先进入，最晚退出。
     * <p>2. 相同优先级的插件，根据{@link Janus#plugins}的配置顺序决定其先后顺序
     */
    default int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    /**
     * 分流。
     */
    default void switchBranch(JanusContext context) {
        // 默认空实现
    }

    /**
     * primary 方法执行前
     */
    default void beforePrimaryExecute(JanusContext context) {
        // 默认空实现
    }

    /**
     * primary 方法执行后
     * <p>可以用于查询落表结果
     */
    default void afterPrimaryExecute(JanusContext context) {
        // 默认空实现
    }

    /**
     * secondary 方法执行前
     */
    default void beforeSecondaryExecute(JanusContext context) {
        // 默认空实现
    }

    /**
     * secondary 方法执行后
     * <p>可以用于查询落表结果
     */
    default void afterSecondaryExecute(JanusContext context) {
        // 默认空实现
    }

    /**
     * 比对前
     * <p>可以用于对结果进行预处理（例如忽略时间戳字段、对列表进行排序等）
     */
    default void beforeCompare(JanusContext context) {
        // 默认空实现
    }

    /**
     * 比对后
     * <p>比对已完成，结果已知。
     * <p>可以用于异步打印日志、发送报警、落库统计
     */
    default void afterCompare(JanusContext context) {
        // 默认空实现
    }
}
