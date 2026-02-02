package com.ethan.janus.core.lifecycle;

import org.springframework.stereotype.Component;

@Component
public class LifecycleDecoratorManager implements Lifecycle {

    private final Lifecycle decoratedLifecycle;

    public LifecycleDecoratorManager(CoreLifecycle coreLifecycle) {
        Lifecycle transactionRollbackLifecycle = new TransactionRollbackLifecycle(coreLifecycle);
        this.decoratedLifecycle = new PluginsExecuteLifecycle(transactionRollbackLifecycle);
    }

    @Override
    public void switchBranch(JanusContext context) {
        decoratedLifecycle.switchBranch(context);
    }

    @Override
    public void primaryExecute(JanusContext context) {
        decoratedLifecycle.primaryExecute(context);
    }

    @Override
    public void secondaryExecute(JanusContext context) {
        decoratedLifecycle.secondaryExecute(context);
    }

    @Override
    public void compare(JanusContext context) {
        decoratedLifecycle.compare(context);
    }
}
