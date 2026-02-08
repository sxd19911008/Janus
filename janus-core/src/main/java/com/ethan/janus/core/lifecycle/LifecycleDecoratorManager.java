package com.ethan.janus.core.lifecycle;

import com.ethan.janus.core.aspect.JanusAspect;
import com.ethan.janus.core.dto.JanusContextImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 用于组合组合所有生命周期，并且注入{@link JanusAspect}
 * <p>注意，该类不可添加任何额外功能，仅用于组合装饰模式的所有实现
 */
@Component
public class LifecycleDecoratorManager extends LifecycleDecorator {

    @Autowired
    private CoreLifecycle coreLifecycle;
    @Autowired
    private RollbackLifecycle rollbackLifecycle;
    @Autowired
    private HigherPluginsExecuteLifecycle higherPluginsExecuteLifecycle;
    @Autowired
    private LowerPluginsExecuteLifecycle lowerPluginsExecuteLifecycle;

    /**
     * 装饰模式，组合所有生命周期
     */
    @PostConstruct
    public void postConstruct() {
        this.setDecoratedLifecycle(higherPluginsExecuteLifecycle)
                .setDecoratedLifecycle(rollbackLifecycle)
                .setDecoratedLifecycle(lowerPluginsExecuteLifecycle)
                .setDecoratedLifecycle(coreLifecycle);
    }

    @Override
    public void switchBranch(JanusContextImpl context) {
        decoratedLifecycle.switchBranch(context);
    }

    @Override
    public void primaryExecute(JanusContextImpl context) {
        decoratedLifecycle.primaryExecute(context);
    }

    @Override
    public void secondaryExecute(JanusContextImpl context) {
        decoratedLifecycle.secondaryExecute(context);
    }

    @Override
    public void compare(JanusContextImpl context) {
        decoratedLifecycle.compare(context);
    }
}
