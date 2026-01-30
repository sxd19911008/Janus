package com.ethan.janus.starter.service;

import com.ethan.janus.core.annotation.Secondary;
import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;
import org.springframework.stereotype.Service;

@Secondary
@Service
public class SecondaryService implements TestInterface {

    @Override
    public TestResponse testMethod(TestRequest request) {
        if ("1".equals(request.getKey())) {
            return new TestResponse(1);
        } else if ("2".equals(request.getKey())) {
            return new TestResponse(3);
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new TestResponse(0);
    }
}
