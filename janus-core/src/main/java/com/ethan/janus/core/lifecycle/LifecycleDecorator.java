package com.ethan.janus.core.lifecycle;

public abstract class LifecycleDecorator implements Lifecycle {

    protected Lifecycle decoratedLifecycle;

    public LifecycleDecorator(Lifecycle decoratedLifecycle) {
        this.decoratedLifecycle = decoratedLifecycle;
    }
}
