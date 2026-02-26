package com.ethan.janus.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

/**
 * JacksonCompareUtil 单元测试
 */
public class CompareJsonTest {

    static Stream<Arguments> testCompareJsonDataProvider() {
        return Stream.of(
                Arguments.of(
                        "{\"wrapper\":{\"numInt\":100.00,\"numDecimal\":99.98,\"str\":\"world\",\"bool\":false,\"nullVal\":\"notNull\",\"extra\":\"unexpected\",\"date\":\"2023-01-02 12:00:01\",\"datePass\":\"2023-01-01 12:57:34\",\"intPass\":10,\"strPass\":\"ok\",\"boolPass\":false},\"list\":[{\"val\":\"A\",\"id\":1},{\"id\":2}],\"list2\":[{\"id\":1},{\"id\":2},{\"id\":3}],\"intB\":124}",
                        "{\"wrapper\":{\"null1\":null,\"null2\":\"null\",\"numInt\":100,\"numDecimal\":99.99,\"str\":\"hello\",\"bool\":true,\"nullVal\":null,\"aaa\":null,\"date\":\"2023-01-02 12:00:00\",\"datePass\":\"2023-01-01 12:57:34\",\"intPass\":10,\"strPass\":\"ok\",\"boolPass\":false},\"list\":[{\"id\":1,\"val\":\"A\"},{\"id\":2,\"val\":\"B\"}],\"list2\":[{\"id\":1},{\"id\":2}],\"intB\":123}",
                        null,
                        "{\"intB\":\"124 / 123\",\"list2\":\"size[3] / size[2]\",\"wrapper.date\":\"2023-01-02 12:00:01 / 2023-01-02 12:00:00\",\"wrapper.nullVal\":\"notNull / null\",\"wrapper.bool\":\"false / true\",\"wrapper.null2\":\"null / notNull\",\"wrapper.numDecimal\":\"99.98 / 99.99\",\"wrapper.str\":\"world / hello\",\"wrapper.extra\":\"notNull / null\",\"list[1].val\":\"null / notNull\"}"
                ),
                Arguments.of(
                        "",
                        null,
                        null,
                        "{\"\":\"null / null\"}"
                ),
                Arguments.of(
                        null,
                        "{\"aa\":\"bb\"}",
                        null,
                        "{\"\":\"null / notNull\"}"
                ),
                Arguments.of(
                        // 场景1：忽略根级别字段
                        "{\"name\":\"John\", \"age\":30}",
                        "{\"name\":\"John\", \"age\":31}",
                        new HashSet<>(Collections.singletonList("age")),
                        "{}"
                ),
                Arguments.of(
                        // 场景2：忽略嵌套字段
                        "{\"wrapper\":{\"status\":\"ok\", \"code\":200}}",
                        "{\"wrapper\":{\"status\":\"fail\", \"code\":500}}",
                        new HashSet<>(Arrays.asList("wrapper.status", "wrapper.code")),
                        "{}"
                ),
                Arguments.of(
                        // 场景3：忽略列表中的字段
                        "{\"list\":[{\"id\":1, \"val\":\"A\"}, {\"id\":2, \"val\":\"B\"}]}",
                        "{\"list\":[{\"id\":1, \"val\":\"X\"}, {\"id\":2, \"val\":\"Y\"}]}",
                        new HashSet<>(Arrays.asList("list[0].val", "list[1].val")),
                        "{}"
                ),
                Arguments.of(
                        // 场景4：忽略部分差异，保留其他差异
                        "{\"a\":1, \"b\":2}",
                        "{\"a\":2, \"b\":3}",
                        new HashSet<>(Collections.singletonList("a")),
                        "{\"b\":\"2 / 3\"}"
                ),
                Arguments.of(
                        // 场景5：忽略不存在的字段（不应报错）
                        "{\"a\":1}",
                        "{\"a\":2}",
                        new HashSet<>(Collections.singletonList("z")),
                        "{\"a\":\"1 / 2\"}"
                ),
                Arguments.of(
                        // 场景6：忽略 null vs notNull
                        "{\"a\":null}",
                        "{\"a\":1}",
                        new HashSet<>(Collections.singletonList("a")),
                        "{}"
                )
        );
    }

    @ParameterizedTest(name = "案例 {index}: 用户名={0}, 预期结果={2}")
    @MethodSource("testCompareJsonDataProvider")
    public void testCompareJson(String actualJson, String expectJson, Set<String> ignoreFieldPaths, String expectedStr) {
        // 测试比对
        Map<String, String> actualMap = JanusJsonUtils.compareJson(actualJson, expectJson, ignoreFieldPaths);
        System.out.println("比对结果 Map=" + JanusJsonUtils.writeValueAsString(actualMap));

        // 解析预期结果
        Map<String, String> expectedMap = JanusJsonUtils.readValue(expectedStr, new TypeReference<HashMap<String, String>>() {
        });

        // 验证结果
        Set<String> keySet = new HashSet<>();
        keySet.addAll(actualMap.keySet());
        keySet.addAll(expectedMap.keySet());
        for (String key : keySet) {
            String actual = actualMap.get(key);
            String expected = expectedMap.get(key);
            Assertions.assertEquals(expected, actual);
        }
    }

    static Stream<Arguments> testCompareObjDataProvider() {
        return Stream.of(
                Arguments.of(
                        null,
                        null,
                        null,
                        "{}"
                ),
                Arguments.of(
                        null,
                        "{\"aa\":\"bb\"}",
                        null,
                        "{\"\":\"null / notNull\"}"
                ),
                Arguments.of(
                        // 场景1：忽略根级别字段
                        "{\"name\":\"John\", \"age\":30}",
                        "{\"name\":\"John\", \"age\":31}",
                        new HashSet<>(Collections.singletonList("age")),
                        "{}"
                ),
                Arguments.of(
                        // 场景4：忽略部分差异，保留其他差异
                        "{\"a\":1, \"b\":2}",
                        "{\"a\":2, \"b\":3}",
                        new HashSet<>(Collections.singletonList("a")),
                        "{\"b\":\"2 / 3\"}"
                ),
                Arguments.of(
                        // 场景5：忽略不存在的字段（不应报错）
                        "{\"a\":1}",
                        "{\"a\":2}",
                        new HashSet<>(Collections.singletonList("z")),
                        "{\"a\":\"1 / 2\"}"
                ),
                Arguments.of(
                        // 场景6：忽略 null vs notNull
                        "{\"a\":null}",
                        "{\"a\":1}",
                        null,
                        "{\"a\":\"null / notNull\"}"
                )
        );
    }

    @ParameterizedTest(name = "案例 {index}: 用户名={0}, 预期结果={2}")
    @MethodSource("testCompareObjDataProvider")
    public void testCompareObj(String actualJson, String expectJson, Set<String> ignoreFieldPaths, String expectedStr) {
        // 测试比对
        Map<String, String> actualMap = JanusJsonUtils.compareObj(this.stringToMap(actualJson), this.stringToMap(expectJson), ignoreFieldPaths);
        System.out.println("比对结果 Map=" + JanusJsonUtils.writeValueAsString(actualMap));

        // 解析预期结果
        Map<String, String> expectedMap = JanusJsonUtils.readValue(expectedStr, new TypeReference<HashMap<String, String>>() {
        });

        // 验证结果
        Set<String> keySet = new HashSet<>();
        keySet.addAll(actualMap.keySet());
        keySet.addAll(expectedMap.keySet());
        for (String key : keySet) {
            String actual = actualMap.get(key);
            String expected = expectedMap.get(key);
            Assertions.assertEquals(expected, actual);
        }
    }

    private Map<String, String> stringToMap(String string) {
        if (JanusUtils.isBlank(string)) {
            return null;
        }
        return JanusJsonUtils.readValue(string, new TypeReference<HashMap<String, String>>() {
        });
    }
}
