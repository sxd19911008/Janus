package com.ethan.janus.core.lifecycle;

public abstract class LifecycleDecorator implements Lifecycle {

    protected Lifecycle decoratedLifecycle;

    /**
     * 用set方法进行包装
     *
     * @param decoratedLifecycle 被包装的对象
     * @return 如果被包装对象是 LifecycleDecorator 类型，则将被包装对象返回，方便连续包装
     */
    protected LifecycleDecorator setDecoratedLifecycle(Lifecycle decoratedLifecycle) {
        this.decoratedLifecycle = decoratedLifecycle;
        // 为了方便连续包装，返回被包装的对象
        if (this.decoratedLifecycle instanceof LifecycleDecorator) {
            return (LifecycleDecorator) this.decoratedLifecycle;
        } else {
            return null;
        }
    }
}
