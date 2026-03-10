package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.starter.JanusThrottlingTests;
import org.springframework.stereotype.Component;

@Component
public class CountCompareJanusPlugin implements JanusPlugin {

    @Override
    public void afterCompare(JanusContext context) {
        JanusThrottlingTests.longAdder.increment();
    }
}
