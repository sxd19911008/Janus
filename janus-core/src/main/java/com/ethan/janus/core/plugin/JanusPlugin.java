package com.ethan.janus.core.plugin;

import com.ethan.janus.core.lifecycle.JanusContext;

public interface JanusPlugin<T> {

    /**
     * 分流。
     */
    default void switchBranch(JanusContext<T> context) {
        // 默认空实现
    }

    /**
     * primary 方法执行前
     */
    default void beforePrimaryExecute(JanusContext<T> context) {
        // 默认空实现
    }

    /**
     * primary 方法执行后
     * <p>可以用于查询落表结果
     */
    default void afterPrimaryExecute(JanusContext<T> context) {
        // 默认空实现
    }

    /**
     * secondary 方法执行前
     */
    default void beforeSecondaryExecute(JanusContext<T> context) {
        // 默认空实现
    }

    /**
     * secondary 方法执行后
     * <p>可以用于查询落表结果
     */
    default void afterSecondaryExecute(JanusContext<T> context) {
        // 默认空实现
    }

    /**
     * 比对前
     * <p>可以用于对结果进行预处理（例如忽略时间戳字段、对列表进行排序等）
     */
    default void beforeCompare(JanusContext<T> context) {
        // 默认空实现
    }

    /**
     * 比对后
     * <p>比对已完成，结果已知。
     * <p>可以用于异步打印日志、发送报警、落库统计
     */
    default void afterCompare(JanusContext<T> context) {
        // 默认空实现
    }
}
