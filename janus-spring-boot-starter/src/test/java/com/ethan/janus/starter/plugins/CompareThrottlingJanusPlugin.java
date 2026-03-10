package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

@Component
public class CompareThrottlingJanusPlugin implements JanusPlugin {

    private final LongAdder longAdder = new LongAdder();

    @Override
    public void switchBranch(JanusContext context) {
        int value = longAdder.intValue();
        if (value >= 3) {
            context.setIsCompare(false);
        } else {
            longAdder.increment();
        }
    }
}
