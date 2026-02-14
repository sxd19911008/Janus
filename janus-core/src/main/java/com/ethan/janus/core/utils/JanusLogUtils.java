package com.ethan.janus.core.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Janus 日志格式化工具类
 */
@Slf4j
public class JanusLogUtils {

    public static final String SUCCESS_ICON = "✅";
    public static final String FAIL_ICON = "❌";

    public static String toJsonString(Object object) {
        try {
            if (object instanceof String) {
                return String.valueOf(object);
            } else {
                return JanusJsonUtils.writeValueAsString(object);
            }
        } catch (Throwable e) {
            log.error("[Janus] {} >> 对象序列化JSON字符串报错", FAIL_ICON, e);
        }
        return null;
    }
}