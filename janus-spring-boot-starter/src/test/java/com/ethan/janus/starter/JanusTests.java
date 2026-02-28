package com.ethan.janus.starter;

import com.ethan.janus.core.utils.JanusJsonUtils;
import com.ethan.janus.starter.config.JanusRollbackClearCacheImpl;
import com.ethan.janus.starter.dao.TestRollbackMapper;
import com.ethan.janus.starter.dto.PluginRes;
import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;
import com.ethan.janus.starter.service.TestInterface;
import com.ethan.janus.starter.service.TransactionalService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Janus 框架功能测试
 */
@SpringBootTest
public class JanusTests {

    @Autowired
    private TestInterface testInterface;
    @Autowired
    private TestRollbackMapper testRollbackMapper;
    @Autowired
    private TransactionalService transactionalService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JanusRollbackClearCacheImpl janusRollbackClearCache;

    public static PluginRes pluginRes;

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {
        // 这里无需写 Bean 方法，保持空即可
    }

    static Stream<Arguments> testSyncCompareDataProvider() {
        return Stream.of(
                Arguments.of(
                        TestRequest.builder().key("1").build(),
                        TestResponse.builder().number(1).build(),
                        "{\"methodId\":\"testSyncCompare\",\"compareRes\":{\"compareStatus\":\"success\"},\"businessKey\":\"1_qqq\",\"testAnnotationKey\":\"Archimonde\"}"
                ),
                Arguments.of(
                        TestRequest.builder().key("2").build(),
                        TestResponse.builder().number(2).build(),
                        "{\"methodId\":\"testSyncCompare\",\"compareRes\":{\"compareStatus\":\"different\",\"diffFieldMap\":{\"res.number\":\"2 / 3\"}},\"businessKey\":\"2_qqq\",\"testAnnotationKey\":\"Archimonde\"}"
                )
        );
    }

    @ParameterizedTest(name = "案例 {index}: requestStr={0}")
    @MethodSource("testSyncCompareDataProvider")
    public void testSyncCompare(TestRequest request, TestResponse responseExpected, String pluginResExpectedStr) {
        /* 整理测试数据 */
        PluginRes pluginResExpected = JanusJsonUtils.readValue(pluginResExpectedStr, new TypeReference<PluginRes>() {
        });
        Map<String, Object> expected = new HashMap<>();
        expected.put("response", responseExpected);
        expected.put("pluginRes", pluginResExpected);

        /* 执行测试方法 */
        pluginRes = new PluginRes();
        TestResponse response = testInterface.testSyncCompare(request);
        System.err.println(JanusJsonUtils.writeValueAsString(pluginRes));

        /* 校验结果 */
        Assertions.assertTrue(pluginRes.primaryTime > 0);
        pluginRes.primaryTime = null;
        Assertions.assertTrue(pluginRes.secondaryTime > 0);
        pluginRes.secondaryTime = null;
        Map<String, Object> actual = new HashMap<>();
        actual.put("response", response);
        actual.put("pluginRes", pluginRes);
        Map<String, String> compareResMap = JanusJsonUtils.compareObj(actual, expected);
        if (!compareResMap.isEmpty()) {
            Assertions.fail(JanusJsonUtils.writeValueAsString(compareResMap));
        }
    }


    static Stream<Arguments> testRollbackOneDataProvider() {
        return Stream.of(
                Arguments.of( // clearCache 单测
                        TestRequest.builder().key("a").build(),
                        TestResponse.builder().number(0).build(),
                        "{\"methodId\":\"testRollbackOne\",\"compareRes\":{\"compareStatus\":\"success\"},\"businessKey\":\"a\",\"resTblNum\":3}"
                )
//                ,
//                Arguments.of(
//                        TestRequest.builder().key("b").build(),
//                        TestResponse.builder().number(2).build(),
//                        ""
//                )
        );
    }

    @ParameterizedTest(name = "案例 {index}: requestStr={0}")
    @MethodSource("testRollbackOneDataProvider")
    public void testRollbackOne1(TestRequest request, TestResponse responseExpected, String pluginResExpectedStr) {
        /* 整理测试数据 */
        PluginRes pluginResExpected = JanusJsonUtils.readValue(pluginResExpectedStr, new TypeReference<PluginRes>() {
        });
        Map<String, Object> expected = new HashMap<>();
        expected.put("response", responseExpected);
        expected.put("pluginRes", pluginResExpected);

        /* 数据库初始化 */
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_rollback");
        jdbcTemplate.execute(
                "CREATE TABLE test_rollback (" +
                        "tbl_key VARCHAR(50)," +
                        "tbl_num INT" +
                        ")"
        );
        jdbcTemplate.update("INSERT INTO test_rollback (tbl_key, tbl_num) VALUES (?, ?)", "exist", 1);
        jdbcTemplate.update("INSERT INTO test_rollback (tbl_key, tbl_num) VALUES (?, ?)", "delete", 1);

        /* 开启一级缓存清理 */
        janusRollbackClearCache.isClearCache = true;

        /* 执行测试方法 */
        pluginRes = new PluginRes();
        TestResponse response = transactionalService.testRollbackOne(request);
        JanusTests.pluginRes.resTblNum = testRollbackMapper.selectNumByKey("exist");
        System.err.println(JanusJsonUtils.writeValueAsString(pluginRes));

        /* 校验结果 */
        Assertions.assertTrue(pluginRes.primaryTime > 0);
        pluginRes.primaryTime = null;
        Assertions.assertTrue(pluginRes.secondaryTime > 0);
        pluginRes.secondaryTime = null;
        Map<String, Object> actual = new HashMap<>();
        actual.put("response", response);
        actual.put("pluginRes", pluginRes);
        Map<String, String> compareResMap = JanusJsonUtils.compareObj(actual, expected);
        if (!compareResMap.isEmpty()) {
            Assertions.fail(JanusJsonUtils.writeValueAsString(compareResMap));
        }
    }

    @ParameterizedTest(name = "案例 {index}: requestStr={0}")
    @MethodSource("testRollbackOneDataProvider")
    public void testRollbackOne2(TestRequest request, TestResponse responseExpected, String pluginResExpectedStr) {
        /* 整理测试数据 */
        PluginRes pluginResExpected = JanusJsonUtils.readValue(pluginResExpectedStr, new TypeReference<PluginRes>() {
        });
        Map<String, Object> expected = new HashMap<>();
        expected.put("response", responseExpected);
        expected.put("pluginRes", pluginResExpected);

        /* 数据库初始化 */
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_rollback");
        jdbcTemplate.execute(
                "CREATE TABLE test_rollback (" +
                        "tbl_key VARCHAR(50)," +
                        "tbl_num INT" +
                        ")"
        );
        // 由于 testRollbackOne1 方法中，多套了一层 TransactionalService 导致多加了1，这里初始化写成2保持期望值一致。
        jdbcTemplate.update("INSERT INTO test_rollback (tbl_key, tbl_num) VALUES (?, ?)", "exist", 2);
        jdbcTemplate.update("INSERT INTO test_rollback (tbl_key, tbl_num) VALUES (?, ?)", "delete", 1);

        /* 关闭一级缓存清理 */
        /*
         * 由于没有上层事务，primary 和 secondary 分支是2个不同的事务，不共享session。这里关闭 clearCache 也可以正常运行。
         * 真正使用框架时，不要关闭 clearCache。
         */
        janusRollbackClearCache.isClearCache = false;

        /* 执行测试方法 */
        pluginRes = new PluginRes();
        TestResponse response = testInterface.testRollbackOne(request);
        JanusTests.pluginRes.resTblNum = testRollbackMapper.selectNumByKey("exist");
        System.err.println(JanusJsonUtils.writeValueAsString(pluginRes));

        /* 校验结果 */
        Assertions.assertTrue(pluginRes.primaryTime > 0);
        pluginRes.primaryTime = null;
        Assertions.assertTrue(pluginRes.secondaryTime > 0);
        pluginRes.secondaryTime = null;
        Map<String, Object> actual = new HashMap<>();
        actual.put("response", response);
        actual.put("pluginRes", pluginRes);
        Map<String, String> compareResMap = JanusJsonUtils.compareObj(actual, expected);
        if (!compareResMap.isEmpty()) {
            Assertions.fail(JanusJsonUtils.writeValueAsString(compareResMap));
        }
    }
}
