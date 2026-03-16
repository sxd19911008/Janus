package com.eredar.janus.core.lifecycle;

import com.eredar.janus.core.dto.JanusContextImpl;
import org.springframework.stereotype.Component;

/**
 * 统计分支耗时，精确到纳秒
 */
@Component
public class TimeLifecycle extends LifecycleDecorator {

    @Override
    public void switchBranch(JanusContextImpl context) {
        decoratedLifecycle.switchBranch(context);
    }

    @Override
    public void primaryExecute(JanusContextImpl context) {
        long time = System.nanoTime();
        decoratedLifecycle.primaryExecute(context);
        time = System.nanoTime() - time;
        context.setPrimaryTime(time);
    }

    @Override
    public void secondaryExecute(JanusContextImpl context) {
        long time = System.nanoTime();
        decoratedLifecycle.secondaryExecute(context);
        time = System.nanoTime() - time;
        context.setSecondaryTime(time);
    }

    @Override
    public void compare(JanusContextImpl context) {
        decoratedLifecycle.compare(context);
    }
}
