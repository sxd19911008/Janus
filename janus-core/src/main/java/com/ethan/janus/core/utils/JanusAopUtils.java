package com.ethan.janus.core.utils;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Field;

/**
 * AOP 相关工具
 */
public class JanusAopUtils {

    /**
     * 获取 Spring AOP 动态代理对象的 target 对象
     *
     * @param proxy Spring AOP 动态代理对象
     * @return 代理对象的 target 对象
     */
    public static Object getProxyTarget(Object proxy) {
        if (AopUtils.isAopProxy(proxy)) {
            Object target = AopProxyUtils.getSingletonTarget(proxy);
            if (target == null) {
                if (AopUtils.isJdkDynamicProxy(proxy)) {
                    target = getJdkDynamicProxyTarget(proxy);
                } else {
                    target = getCglibProxyTarget(proxy);
                }
            }
            if (AopUtils.isAopProxy(target)) {
                return getProxyTarget(target);
            } else {
                return target;
            }
        } else {
            return proxy;
        }
    }

    /**
     * 根据 JDK 动态代理生成的代理对象，获取代理的目标对象
     *
     * @param proxy JDK 动态代理生成的代理对象
     * @return 代理的目标对象
     */
    private static Object getJdkDynamicProxyTarget(Object proxy) {
        try {
            Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
            h.setAccessible(true);
            AopProxy aopProxy = (AopProxy) h.get(proxy);
            Field advised = aopProxy.getClass().getDeclaredField("advised");
            advised.setAccessible(true);
            AdvisedSupport advisedSupport = (AdvisedSupport) advised.get(aopProxy);
            return advisedSupport.getTargetSource().getTarget();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据 CGLIB 动态代理生成的代理对象，获取代理的目标对象
     *
     * @param proxy CGLIB 动态代理生成的代理对象
     * @return 代理的目标对象
     */
    private static Object getCglibProxyTarget(Object proxy) {
        try {
            Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
            h.setAccessible(true);
            Object dynamicAdvisedInterceptor = h.get(proxy);
            Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
            advised.setAccessible(true);
            AdvisedSupport advisedSupport = (AdvisedSupport) advised.get(dynamicAdvisedInterceptor);
            return advisedSupport.getTargetSource().getTarget();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
