package com.ethan.janus.starter.service;

import com.ethan.janus.core.annotation.Janus;
import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;
import com.ethan.janus.starter.plugins.ExecuteTimeCountJanusPlugin;
import com.ethan.janus.starter.plugins.SwitchJanusPlugin;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class PrimaryService implements TestInterface {

    @Janus(compareType = CompareType.SYNC_COMPARE, plugins = {SwitchJanusPlugin.class, ExecuteTimeCountJanusPlugin.class})
    @Override
    public TestResponse testMethod(TestRequest request) {
        if ("1".equals(request.getKey())) {
            return new TestResponse(1);
        } else if ("2".equals(request.getKey())) {
            return new TestResponse(2);
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new TestResponse(0);
    }
}
