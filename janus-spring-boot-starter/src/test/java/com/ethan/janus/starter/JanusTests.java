package com.ethan.janus.starter;

import com.ethan.janus.core.utils.JanusJsonUtils;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Janus 框架功能测试
 */
@SpringBootTest(classes = JanusTestApplication.class)
public class JanusTests {

    @Autowired
    private TestInterface testInterface;
    @Autowired
    private TestRollbackMapper testRollbackMapper;
    @Autowired
    private TransactionalService transactionalService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static PluginRes pluginRes;
    public static String globalString;

    static Stream<Arguments> testSyncCompareDataProvider() {
        return Stream.of(
                Arguments.of(
                        TestRequest.builder().key("1").build(),
                        TestResponse.builder().number(1).build(),
                        "{\"methodId\":\"testSyncCompare\",\"masterBranchName\":\"primary\",\"compareRes\":{\"compareStatus\":\"success\"},\"businessKey\":\"1_qqq\",\"testAnnotationKey\":\"Archimonde\"}"
                ),
                Arguments.of(
                        TestRequest.builder().key("2").build(),
                        TestResponse.builder().number(2).build(),
                        "{\"methodId\":\"testSyncCompare\",\"masterBranchName\":\"primary\",\"compareRes\":{\"compareStatus\":\"different\",\"diffFieldMap\":{\"res.number\":\"2 / 3\"}},\"businessKey\":\"2_qqq\",\"testAnnotationKey\":\"Archimonde\"}"
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
        Assertions.assertEquals("Global", globalString);
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
                        "{\"methodId\":\"testRollbackOne\",\"masterBranchName\":\"secondary\",\"compareRes\":{\"compareStatus\":\"success\"},\"businessKey\":\"a\",\"resTblNum\":3}"
                ),
                Arguments.of(
                        TestRequest.builder().key("compareBranch_err").build(),
                        TestResponse.builder().number(0).build(),
                        "{\"methodId\":\"testRollbackOne\",\"masterBranchName\":\"secondary\",\"compareRes\":{\"compareStatus\":\"primary_error\"},\"businessKey\":\"compareBranch_err\",\"resTblNum\":3}"
                )
                ,
                Arguments.of(
                        TestRequest.builder().key("masterBranch_err").build(),
                        null,
                        "{\"methodId\":\"testRollbackOne\",\"masterBranchName\":\"secondary\",\"compareRes\":{\"compareStatus\":\"secondary_error\"},\"businessKey\":\"masterBranch_err\",\"resTblNum\":1}"
                )
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

        /* 执行测试方法 */
        pluginRes = new PluginRes();
        TestResponse response = null;
        try {
            response = transactionalService.testRollbackOne(request);
        } catch (Throwable e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
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
        jdbcTemplate.update("INSERT INTO test_rollback (tbl_key, tbl_num) VALUES (?, ?)", "exist", 1);
        jdbcTemplate.update("INSERT INTO test_rollback (tbl_key, tbl_num) VALUES (?, ?)", "delete", 1);

        /* 执行测试方法 */
        pluginRes = new PluginRes();
        TestResponse response = null;
        try {
            response = testInterface.testRollbackOne(request);
            // 由于1方法中，多套了一层 TransactionalService 导致多加了1，这里+1保持期望值一致。
            jdbcTemplate.update("UPDATE test_rollback SET tbl_num = tbl_num + 1 WHERE tbl_key = ?", "exist");
        } catch (Throwable e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
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


    static Stream<Arguments> testRollbackAllDataProvider() {
        return Stream.of(
                Arguments.of( // clearCache 单测
                        TestRequest.builder().key("a").build(),
                        TestResponse.builder().number(0).build(),
                        "{\"methodId\":\"testRollbackAll\",\"masterBranchName\":\"secondary\",\"compareRes\":{\"compareStatus\":\"success\"},\"businessKey\":\"a\",\"resTblNum\":2}"
                ),
                Arguments.of(
                        TestRequest.builder().key("compareBranch_err").build(),
                        TestResponse.builder().number(0).build(),
                        "{\"methodId\":\"testRollbackAll\",\"masterBranchName\":\"secondary\",\"compareRes\":{\"compareStatus\":\"primary_error\"},\"businessKey\":\"compareBranch_err\",\"resTblNum\":2}"
                ),
                Arguments.of(
                        TestRequest.builder().key("masterBranch_err").build(),
                        null,
                        "{\"methodId\":\"testRollbackAll\",\"masterBranchName\":\"secondary\",\"compareRes\":{\"compareStatus\":\"secondary_error\"},\"businessKey\":\"masterBranch_err\",\"resTblNum\":1}"
                )
        );
    }

    @ParameterizedTest(name = "案例 {index}: requestStr={0}")
    @MethodSource("testRollbackAllDataProvider")
    public void testRollbackAll1(TestRequest request, TestResponse responseExpected, String pluginResExpectedStr) {
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

        /* 执行测试方法 */
        pluginRes = new PluginRes();
        TestResponse response = null;
        try {
            response = transactionalService.testRollbackAll(request);
        } catch (Throwable e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
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
    @MethodSource("testRollbackAllDataProvider")
    public void testRollbackAll2(TestRequest request, TestResponse responseExpected, String pluginResExpectedStr) {
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

        /* 执行测试方法 */
        pluginRes = new PluginRes();
        TestResponse response = null;
        try {
            response = testInterface.testRollbackAll(request);
            // 由于1方法中，多套了一层 TransactionalService 导致多加了1，这里+1保持期望值一致。
            jdbcTemplate.update("UPDATE test_rollback SET tbl_num = tbl_num + 1 WHERE tbl_key = ?", "exist");
        } catch (Throwable e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
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


    static Stream<Arguments> testIgnoreDataProvider() {
        return Stream.of(
                Arguments.of(
                        "{\"methodId\":\"testIgnore\",\"masterBranchName\":\"secondary\",\"compareRes\":{\"compareStatus\":\"success\"},\"businessKey\":\"\"}"
                ),
                Arguments.of(
                        "{\"methodId\":\"testIgnore\",\"masterBranchName\":\"secondary\",\"compareRes\":{\"compareStatus\":\"success\"},\"businessKey\":\"\"}"
                )
        );
    }

    @ParameterizedTest(name = "案例 {index}")
    @MethodSource("testIgnoreDataProvider")
    public void testIgnore(String pluginResExpectedStr) {
        /* 整理测试数据 */
        PluginRes pluginResExpected = JanusJsonUtils.readValue(pluginResExpectedStr, new TypeReference<PluginRes>() {
        });

        /* 执行测试方法 */
        pluginRes = new PluginRes();
        TestResponse testResponse = testInterface.testIgnore();
        System.err.println(JanusJsonUtils.writeValueAsString(pluginRes));

        /* 校验结果 */
        Assertions.assertNotNull(testResponse);
        Assertions.assertTrue(pluginRes.primaryTime > 0);
        pluginRes.primaryTime = null;
        Assertions.assertTrue(pluginRes.secondaryTime > 0);
        pluginRes.secondaryTime = null;
        Map<String, String> compareResMap = JanusJsonUtils.compareObj(pluginRes, pluginResExpected);
        if (!compareResMap.isEmpty()) {
            Assertions.fail(JanusJsonUtils.writeValueAsString(compareResMap));
        }
    }
}
