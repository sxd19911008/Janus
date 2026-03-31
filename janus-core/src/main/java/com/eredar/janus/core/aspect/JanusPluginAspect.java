package com.eredar.janus.core.aspect;

import com.eredar.janus.core.dto.JanusContext;
import com.eredar.janus.core.utils.JanusLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * 异常捕获切面，用于防止不重要的异常导致中断比对流程
 * <p>1. 可以防止影响正常功能
 * <p>2. 可以防止插件等附加功能之间互相影响
 */
@Slf4j
@Aspect
public class JanusPluginAspect {

    // 切所有JanusPlugin类型的target对象的类(原本的bean，不包括父类和代理类)
    @Pointcut("target(com.eredar.janus.core.plugin.JanusPlugin)" +
            " && !within(com.eredar.janus.core.plugin..*)" +
            " && execution(public * *(..))" +
            " && !execution(* getOrder())")
    public void allPluginsMethods() {
    }

    @Around("allPluginsMethods()")
    public Object janusPluginAspect(ProceedingJoinPoint joinPoint) {
        long time = 0;
        try {
            // 统计耗时
            if (log.isTraceEnabled()) {
                time = System.nanoTime();
            }
            // 执行方法
            Object res = joinPoint.proceed();
            // 统计耗时
            if (log.isTraceEnabled()) {
                time = System.nanoTime() - time;
                // 不允许因为打印日志导致报错
                try {
                    // 获取切点方法入参
                    Object[] args = joinPoint.getArgs();
                    // 获取签名对象
                    Signature signature = joinPoint.getSignature();
                    // 获取切点类名
                    String pluginClassName = signature.getDeclaringTypeName();
                    // 获取方法名称
                    String methodName = signature.getName();
                    if (args != null && args.length > 0 && args[0] instanceof JanusContext) {
                        JanusContext context = (JanusContext) args[0];
                        log.trace(
                                "[Janus] {} [methodId:{}] [businessKey:{}] [plugin:{}] [methodName:{}] >> timeTaken={}ns",
                                JanusLogUtils.SUCCESS_ICON,
                                context.getMethodId(),
                                context.getBusinessKey(),
                                pluginClassName,
                                methodName,
                                time
                        );
                    } else {
                        log.trace(
                                "[Janus] {} [plugin:{}] [methodName:{}] >> timeTaken={}ns",
                                JanusLogUtils.SUCCESS_ICON,
                                pluginClassName,
                                methodName,
                                time
                        );
                    }
                } catch (Throwable e) {
                    log.error(
                            "[Janus] {} >> error when log janusPlugin timeTaken",
                            JanusLogUtils.FAIL_ICON,
                            e
                    );
                }
            }
            // 返回结果
            return res;
        } catch (Throwable e) {
            // 捕获日志并且打印日志，不可抛出日志导致流程中断
            try {
                // 获取切点方法入参
                Object[] args = joinPoint.getArgs();
                // 获取签名对象
                Signature signature = joinPoint.getSignature();
                // 获取切点类名
                String pluginClassName = signature.getDeclaringTypeName();
                // 获取方法名称
                String methodName = signature.getName();
                if (args != null && args.length > 0 && args[0] instanceof JanusContext) {
                    JanusContext context = (JanusContext) args[0];
                    log.error(
                            "[Janus] {} [methodId:{}] [businessKey:{}] [plugin:{}] [methodName:{}] >> exception=",
                            JanusLogUtils.FAIL_ICON,
                            context.getMethodId(),
                            context.getBusinessKey(),
                            pluginClassName,
                            methodName,
                            e
                    );
                } else {
                    log.error(
                            "[Janus] {} [plugin:{}] [methodName:{}] >> exception=",
                            JanusLogUtils.FAIL_ICON,
                            pluginClassName,
                            methodName,
                            e
                    );
                }
            } catch (Throwable e1) {
                log.error(
                        "[Janus] {} >>  plugin error",
                        JanusLogUtils.FAIL_ICON,
                        e
                );
                log.error(
                        "[Janus] {} >> get JanusContext error",
                        JanusLogUtils.FAIL_ICON,
                        e1
                );
            }
        }
        return null;
    }
}
