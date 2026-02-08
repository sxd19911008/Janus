package com.ethan.janus.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 异常捕获切面，用于防止不重要的异常导致中断比对流程
 * <p>1. 可以防止影响正常功能
 * <p>2. 可以防止插件等附加功能之间互相影响
 */
@Component
@Aspect
public class JanusCatchAspect {

    // 假设你的接口全路径是 com.example.service.UserService
    // UserService+ 表示匹配 UserService 接口本身及其所有实现类
    @Pointcut("within(com.ethan.janus.core.plugin.JanusPlugin+)")
    public void allPluginsMethods() {}

    @Around("allPluginsMethods()")
    public Object janusAspect(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 捕获日志并且打印日志，不可抛出日志导致流程中断
            // TODO 日志框架
            e.printStackTrace();
        }
        return null;
    }
}
