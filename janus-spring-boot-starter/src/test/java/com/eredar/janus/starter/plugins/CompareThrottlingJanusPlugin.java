package com.eredar.janus.starter.plugins;

import com.eredar.janus.core.dto.JanusContext;
import com.eredar.janus.core.plugin.JanusPlugin;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

@Component
public class CompareThrottlingJanusPlugin implements JanusPlugin {

    private final LongAdder longAdder = new LongAdder();

    @Override
    public void switchBranch(JanusContext context) {
        int value = longAdder.intValue();
        if (value >= 3) {
            context.setNeedCompare(false);
        } else {
            longAdder.increment();
        }
    }
}
