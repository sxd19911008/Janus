package com.ethan.janus.starter;

import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;
import com.ethan.janus.starter.service.TestInterface;
import com.ethan.janus.core.utils.JsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Map;

/**
 * Starter 基础加载测试。
 * <p>
 * 该测试仅验证：引入 Starter 后，自动装配入口可以正常创建 Spring 上下文。
 * 不依赖业务应用类，从而保证 Starter 在被任何工程引入时都可加载。
 */
@SpringBootTest(classes = {
        JanusAutoConfiguration.class,
        JanusTests.TestConfig.class
})
public class JanusTests {

    @Autowired
    private TestInterface testInterface;

    public static Long primaryTime = null;
    public static Long secondaryTime = null;
    public static Map<String, String> compareResMap = null;

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    // 说明：只扫描测试包下的 Service，避免扫描范围过大
    static class TestConfig {
        // 这里无需写 Bean 方法，保持空即可
    }

    @Test
    public void janusTest() {
        TestResponse response1 = testInterface.testMethod(new TestRequest("1"));
        Assertions.assertEquals(1, response1.getNumber());
        Assertions.assertTrue(primaryTime > 0);
        Assertions.assertTrue(secondaryTime > 0);
        Assertions.assertEquals("{}", JsonUtils.writeValueAsString(compareResMap));

        TestResponse response2 = testInterface.testMethod(new TestRequest("2"));
        Assertions.assertEquals(2, response2.getNumber());
        Assertions.assertTrue(primaryTime > 0);
        Assertions.assertTrue(secondaryTime > 0);
        Assertions.assertEquals("{\"number\":\"2 / 3\"}", JsonUtils.writeValueAsString(compareResMap));
    }
}
