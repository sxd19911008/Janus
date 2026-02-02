package com.ethan.janus.core.lifecycle;

/**
 * Janus 生命周期接口
 */
public interface Lifecycle {

    void switchBranch(JanusContext context);

    void primaryExecute(JanusContext context);

    void secondaryExecute(JanusContext context);

    void compare(JanusContext context);
}
