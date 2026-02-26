package com.ethan.janus.core.utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
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
    public static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
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
    public static boolean isNotBlank(final CharSequence cs) {
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
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
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

    /**
     * 获取切点方法
     */
    public static Method getMethodFromJoinPoint(ProceedingJoinPoint joinPoint) {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取具体的方法对象
        return signature.getMethod();
    }

    /**
     * 获取切点方法上面的指定注解
     *
     * @param joinPoint       切点
     * @param annotationClass 注解类
     * @param <T>             注解类型泛型
     * @return 注解对象
     */
    public static <T extends Annotation> T getAnnotation(ProceedingJoinPoint joinPoint, Class<T> annotationClass) {
        // 获取切点方法
        Method method = getMethodFromJoinPoint(joinPoint);

        T annotation = AnnotationUtils.findAnnotation(method, annotationClass);
        if (annotation == null) {
            // 获取实现类的具体 Class
            Class<?> targetClass = joinPoint.getTarget().getClass();

            // 找到实现类中对应的方法
            Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);

            // 从具体方法上拿注解
            annotation = AnnotationUtils.findAnnotation(specificMethod, annotationClass);
        }
        return annotation;
    }
}
