package com.ethan.janus.core.lifecycle;

public class TransactionRollbackLifecycle extends LifecycleDecorator {

    public TransactionRollbackLifecycle(Lifecycle decoratedLifecycle) {
        super(decoratedLifecycle);
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
