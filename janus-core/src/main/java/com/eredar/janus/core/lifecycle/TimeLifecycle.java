package com.eredar.janus.core.lifecycle;

import com.eredar.janus.core.dto.JanusContextImpl;
import com.eredar.janus.core.utils.JanusLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 统计分支耗时，精确到纳秒
 */
@Slf4j
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
        log.debug(
                "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:primaryExecute] >> timeTaken={}ns",
                JanusLogUtils.SUCCESS_ICON,
                context.getMethodId(),
                context.getBusinessKey(),
                time
        );
    }

    @Override
    public void secondaryExecute(JanusContextImpl context) {
        long time = System.nanoTime();
        decoratedLifecycle.secondaryExecute(context);
        time = System.nanoTime() - time;
        context.setSecondaryTime(time);
        log.debug(
                "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:secondaryExecute] >> timeTaken={}ns",
                JanusLogUtils.SUCCESS_ICON,
                context.getMethodId(),
                context.getBusinessKey(),
                time
        );
    }

    @Override
    public void compare(JanusContextImpl context) {
        decoratedLifecycle.compare(context);
    }
}
