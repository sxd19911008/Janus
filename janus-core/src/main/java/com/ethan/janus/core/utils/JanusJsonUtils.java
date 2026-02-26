package com.ethan.janus.core.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import lombok.SneakyThrows;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

public class JanusJsonUtils {

    private static final ObjectMapper ob;
    private static final SimpleDateFormat sdf;

    public static final String NULL = "null";
    public static final String NOT_NULL = "notNull";
    public static final String SEPARATOR = " / ";

    static {
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));

        ob = new ObjectMapper();
        // 忽略为 null 的字段
        ob.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 禁用时间戳
        ob.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 时区
        ob.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
        // 日期格式
        ob.setDateFormat(sdf);
        // 金融场景最好开启，增强浮点类型精度
        ob.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    @SneakyThrows
    public static <T> T readValue(String jsonString, TypeReference<T> type) {
        return ob.readValue(jsonString, type);
    }

    @SneakyThrows
    public static <T> T readValue(String jsonString, Class<T> type) {
        return ob.readValue(jsonString, type);
    }

    @SneakyThrows
    public static <T> T readValue(File file, TypeReference<T> type) {
        return ob.readValue(file, type);
    }

    @SneakyThrows
    public static <T> T treeToValue(JsonNode jsonNode, Class<T> valueType) {
        return ob.treeToValue(jsonNode, valueType);
    }

    @SneakyThrows
    public static String writeValueAsString(Object object) {
        return ob.writeValueAsString(object);
    }

    @SneakyThrows
    public static void writeValue(File file, Object value) {
        ob.writeValue(file, value);
    }

    public static <T> T writeThenRead(Object object, TypeReference<T> type) {
        String jsonString = writeValueAsString(object);
        return readValue(jsonString, type);
    }

    public static <T> T writeThenRead(Object object, Class<T> type) {
        String jsonString = writeValueAsString(object);
        return readValue(jsonString, type);
    }

    /**
     * 比对两个对象
     *
     * @param actual 实际对象
     * @param expect 期望对象
     * @return 差异Map，Key 为路径，Value 为差异描述。如果 Map 为空表示一致。
     */
    public static Map<String, String> compareObj(Object actual, Object expect) {
        return compareObj(actual, expect, null);
    }

    /**
     * 比对两个对象，支持忽略指定路径
     *
     * @param actual           实际对象
     * @param expect           期望对象
     * @param ignoreFieldPaths 忽略的字段路径集合
     * @return 差异Map，Key 为路径，Value 为差异描述。如果 Map 为空表示一致。
     */
    public static Map<String, String> compareObj(Object actual, Object expect, Set<String> ignoreFieldPaths) {
        try {
            // 将对象转换为Json字符串后进行比对，利用Jackson的处理能力
            return compareJson(ob.writeValueAsString(actual), ob.writeValueAsString(expect), ignoreFieldPaths);
        } catch (Throwable e) {
            throw new RuntimeException("Jackson 序列化异常", e);
        }
    }

    /**
     * 比对两个 Json 字符串
     *
     * @param actual 实际 Json 串
     * @param expect 期望 Json 串
     * @return 差异 Map，Key 为路径，Value 为差异描述。如果 Map 为空表示一致。
     */
    public static Map<String, String> compareJson(String actual, String expect) {
        return compareJson(actual, expect, null);
    }

    /**
     * 比对两个 Json 字符串，支持忽略指定路径
     *
     * @param actual           实际 Json 串
     * @param expect           期望 Json 串
     * @param ignoreFieldPaths 忽略的字段路径集合
     * @return 差异 Map，Key 为路径，Value 为差异描述。如果 Map 为空表示一致。
     */
    public static Map<String, String> compareJson(String actual, String expect, Set<String> ignoreFieldPaths) {
        Map<String, String> diffMap = new LinkedHashMap<>();
        if (isNotBlank(actual) && isNotBlank(expect)) {
            try {
                JsonNode actualNode = ob.readTree(actual);
                JsonNode expectNode = ob.readTree(expect);
                // 开始递归比对
                compareNodes("", actualNode, expectNode, diffMap, ignoreFieldPaths);
            } catch (Throwable e) {
                throw new RuntimeException("Jackson 比对异常", e);
            }
            return diffMap;
        } else if (isBlank(actual) && isBlank(expect)) {
            diffMap.put("", NULL + SEPARATOR + NULL);
            return diffMap;
        } else if (isNotBlank(actual)) {
            diffMap.put("", NOT_NULL + SEPARATOR + NULL);
            return diffMap;
        } else {
            diffMap.put("", NULL + SEPARATOR + NOT_NULL);
            return diffMap;
        }
    }

    /* ---------------- Internal Logic ---------------- */

    /**
     * 递归比对节点
     *
     * @param path             当前路径
     * @param actual           实际节点
     * @param expect           期望节点
     * @param diffMap          差异结果容器
     * @param ignoreFieldPaths 忽略的字段路径集合
     */
    private static void compareNodes(String path, JsonNode actual, JsonNode expect, Map<String, String> diffMap, Set<String> ignoreFieldPaths) {
        // 检查是否在忽略列表中
        if (ignoreFieldPaths != null && ignoreFieldPaths.contains(path)) {
            return;
        }

        // 检查是否为 null 节点
        // 如果都是 null，认为一致
        if (actual.isNull() && expect.isNull()) {
            return;
        }
        // 如果其中一个是 null
        if (expect.isNull()) {
            diffMap.put(getPath(path), NOT_NULL + SEPARATOR + NULL);
            return;
        }
        if (actual.isNull()) {
            diffMap.put(getPath(path), NULL + SEPARATOR + NOT_NULL);
            return;
        }

        // 根据类型分发处理
        if (actual.isObject() && expect.isObject()) {
            // 对象
            compareObjects(path, (ObjectNode) actual, (ObjectNode) expect, diffMap, ignoreFieldPaths);
        } else if (actual.isArray() && expect.isArray()) {
            // 列表
            compareArrays(path, (ArrayNode) actual, (ArrayNode) expect, diffMap, ignoreFieldPaths);
        } else if (actual.isValueNode() && expect.isValueNode()) {
            // 基本类型
            compareValues(path, (ValueNode) actual, (ValueNode) expect, diffMap);
        } else {
            // 类型不一致
            diffMap.put(getPath(path), actual.getNodeType() + SEPARATOR + expect.getNodeType());
        }
    }

    /**
     * 比对对象节点 (ObjectNode)
     */
    private static void compareObjects(String path, ObjectNode actual, ObjectNode expect, Map<String, String> diffMap, Set<String> ignoreFieldPaths) {
        Set<String> keySet = new HashSet<>();
        actual.fieldNames().forEachRemaining(keySet::add);
        expect.fieldNames().forEachRemaining(keySet::add);

        for (String key : keySet) {
            String currentPath = buildPath(path, key);
            // 提前检查忽略
            if (ignoreFieldPaths != null && ignoreFieldPaths.contains(currentPath)) {
                continue;
            }

            JsonNode actualNode = actual.get(key);
            JsonNode expectNode = expect.get(key);
            if (isNull(actualNode) && isNull(expectNode)) {
                continue;
            } else if (isNull(expectNode)) {
                diffMap.put(currentPath, NOT_NULL + SEPARATOR + NULL);
                continue;
            } else if (isNull(actualNode)) {
                diffMap.put(currentPath, NULL + SEPARATOR + NOT_NULL);
                continue;
            }
            // 递归比对
            compareNodes(currentPath, actualNode, expectNode, diffMap, ignoreFieldPaths);
        }
    }

    /**
     * 比对数组节点 (ArrayNode)
     * 策略：按索引顺序比对，不排序
     */
    private static void compareArrays(String path, ArrayNode actual, ArrayNode expect, Map<String, String> diffMap, Set<String> ignoreFieldPaths) {
        int actualSize = actual.size();
        int expectSize = expect.size();

        if (actualSize != expectSize) {
            diffMap.put(getPath(path), String.format("size[%d] / size[%d]", actualSize, expectSize));
            return;
        }

        for (int i = 0; i < actualSize; i++) {
            String idxPath = path + "[" + i + "]";
            compareNodes(idxPath, actual.get(i), expect.get(i), diffMap, ignoreFieldPaths);
        }
    }

    /**
     * 比对值节点 (ValueNode)
     */
    private static void compareValues(String path, ValueNode actual, ValueNode expect, Map<String, String> diffMap) {
        // 数值特殊处理
        if (actual.isNumber() && expect.isNumber()) {
            BigDecimal a = actual.decimalValue().stripTrailingZeros();
            BigDecimal b = expect.decimalValue().stripTrailingZeros();
            // 使用 compareTo 避免 1.0 和 1.00 被视为不相等
            if (a.compareTo(b) != 0) {
                diffMap.put(getPath(path), a.toPlainString() + SEPARATOR + b.toPlainString());
            }
        } else {
            // 其他值类型 (String, Boolean) 直接比对
            if (!actual.equals(expect)) {
                diffMap.put(getPath(path), actual.asText() + SEPARATOR + expect.asText());
            }
        }
    }

    /**
     * 构建路径工具
     */
    private static String buildPath(String parent, String child) {
        if (parent == null || parent.isEmpty()) {
            return child;
        }
        return parent + "." + child;
    }

    /**
     * 获取路径显示，如果是空字符串则显示 "ROOT" 或其他标识，这里保持为空字符串或者直接返回path
     */
    private static String getPath(String path) {
        return (path == null || path.isEmpty()) ? "ROOT" : path;
    }

    private static boolean isNull(JsonNode node) {
        return node == null || node.isNull();
    }

    /**
     * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace only
     * @since 2.0
     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
     */
    private static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        /*
         * 不可用 string.trim() 来判断字符串是否全是空白字符。
         * 因为 string.trim() 会创建一个新的字符串对象，不划算。
         * 从 java 11 开始，可以用 string.isBlank() 来判断。
         * 当前写法能兼容更多类型，也可以继续使用。
         */
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 字符串是否非空
     */
    private static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Gets a CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     *
     * @param cs
     *            a CharSequence or {@code null}
     * @return CharSequence length or {@code 0} if the CharSequence is
     *         {@code null}.
     * @since 2.4
     * @since 3.0 Changed signature from length(String) to length(CharSequence)
     */
    private static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
}
