package com.ethan.janus.starter;

import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.service.TestInterface;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.atomic.LongAdder;

/**
 * 异步、流量平衡功能 测试
 */
@SpringBootTest(classes = JanusTestApplication.class)
public class CompareThrottlingTests {

    @Autowired
    private TestInterface testInterface;

    public static LongAdder longAdder = new LongAdder();

    @Test
    public void testCompareThrottling() {
        for (int i = 0; i < 20; i++) {
            testInterface.testCompareThrottling(TestRequest.builder().key(String.valueOf(i + 1)).build());
        }

        Assertions.assertEquals(3, longAdder.longValue());
    }
}
