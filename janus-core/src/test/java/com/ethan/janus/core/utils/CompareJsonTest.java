package com.ethan.janus.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
                        "{\"intB\":\"124 / 123\",\"list2\":\"size[3] / size[2]\",\"wrapper.date\":\"2023-01-02 12:00:01 / 2023-01-02 12:00:00\",\"wrapper.nullVal\":\"notNull / null\",\"wrapper.bool\":\"false / true\",\"wrapper.null2\":\"null / notNull\",\"wrapper.numDecimal\":\"99.98 / 99.99\",\"wrapper.str\":\"world / hello\",\"wrapper.extra\":\"notNull / null\",\"list[1].val\":\"null / notNull\"}"
                ),
                Arguments.of(
                        "",
                        null,
                        "{\"\":\"null / null\"}"
                ),
                Arguments.of(
                        null,
                        "{\"aa\":\"bb\"}",
                        "{\"\":\"null / notNull\"}"
                )
        );
    }

    @ParameterizedTest(name = "案例 {index}: 用户名={0}, 预期结果={2}")
    @MethodSource("testCompareJsonDataProvider")
    public void testCompareJson(String actualJson, String expectJson, String expectedStr) {
        // 测试比对
        Map<String, String> actualMap = JanusJsonUtils.compareJson(actualJson, expectJson);
        System.out.println("比对结果 Map=" + JanusJsonUtils.writeValueAsString(actualMap));

        // 解析预期结果
        Map<String, String> expectedMap = JanusJsonUtils.readValue(expectedStr, new TypeReference<HashMap<String, String>>() {});

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
}
