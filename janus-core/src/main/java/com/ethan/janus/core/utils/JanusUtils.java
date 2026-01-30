package com.ethan.janus.core.utils;

import java.util.Collection;
import java.util.Map;

public class JanusUtils {

    /**
     * 默认值工具
     */
    public static <T> T defaultIfNull(final T object, final T defaultValue) {
        return object != null ? object : defaultValue;
    }

    /**
     * 是否是空字符串
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 字符串是否非空
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
}
