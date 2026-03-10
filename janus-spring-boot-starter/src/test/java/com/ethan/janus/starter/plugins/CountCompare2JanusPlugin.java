package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.starter.CompareThrottlingTests;
import com.ethan.janus.starter.dto.TestRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CountCompare2JanusPlugin implements JanusPlugin {

    @Override
    public void afterCompare(JanusContext context) {
        TestRequest request = (TestRequest) context.getArgs()[0];
        log.debug("CountCompare2JanusPlugin：{}", request.getKey());
        CompareThrottlingTests.longAdder.increment();
    }
}
