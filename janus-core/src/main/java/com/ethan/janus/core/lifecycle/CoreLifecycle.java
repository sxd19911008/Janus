package com.ethan.janus.core.lifecycle;

import com.ethan.janus.core.annotation.Secondary;
import com.ethan.janus.core.config.JanusConfig;
import com.ethan.janus.core.constants.JanusConstants;
import com.ethan.janus.core.dto.BranchInfo;
import com.ethan.janus.core.dto.SecondaryMethodInfo;
import com.ethan.janus.core.exception.JanusException;
import com.ethan.janus.core.utils.JanusUtils;
import com.ethan.janus.core.utils.JsonUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Janus 框架核心功能生命周期。包含框架中的所有核心功能。
 */
@Component
public class CoreLifecycle implements JanusLifecycle {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JanusConfig janusConfig;

    private final Map<Class<?>, Object> secondaryBeanCache = new ConcurrentHashMap<>();
    private final Map<Method, Method> secondaryMethodCache = new ConcurrentHashMap<>();
    private Field methodInvocation = null;

    /**
     * 分流
     */
    @Override
    public void switchBranch(JanusContext context) {
        // 分流
        if (JanusUtils.isBlank(context.getMasterBranchName())) {
            // 使用默认的分流方式
            context.setMasterBranchName(janusConfig.getMasterBranch());
        } else {
            // 校验
            if (!JanusConstants.PRIMARY.equals(context.getMasterBranchName())
                    && !JanusConstants.SECONDARY.equals(context.getMasterBranchName())) {
                throw new JanusException("不支持的 masterBranch 类型: [" + context.getMasterBranchName() + "]");
            }
        }
        // 设置主分支和比对分支
        this.setMasterBranch(context);
    }

    /**
     * 执行 primary 分支
     */
    @Override
    public void primaryExecute(JanusContext context) {
        BranchInfo branch = context.getPrimaryBranch();
        // 校验
        if (branch.getIsExecuted() != null && branch.getIsExecuted()) {
            // 执行过了就不要再执行了。
            return;
        }
        // 切点
        ProceedingJoinPoint joinPoint = context.getJoinPoint();
        try {
            Object res;
            // 执行分支方法
            if (branch.getIsAsync() != null && branch.getIsAsync()) {
                // 异步执行切点方法
                if (methodInvocation == null) {
                    methodInvocation = MethodInvocationProceedingJoinPoint.class.getDeclaredField("methodInvocation");
                    methodInvocation.setAccessible(true);
                }
                ReflectiveMethodInvocation reflectiveMethodInvocation = (ReflectiveMethodInvocation) methodInvocation.get(joinPoint);
                res = ExposeInvocationInterceptor.INSTANCE.invoke(reflectiveMethodInvocation.invocableClone());
            } else { // 同步执行，直接 proceed
                res = joinPoint.proceed();
            }
            branch.setRes(res);
        } catch (Throwable e) {
            // 保存异常对象
            branch.setException(e);
        }
    }

    /**
     * 执行 secondary 分支
     */
    @Override
    public void secondaryExecute(JanusContext context) {
        BranchInfo branch = context.getSecondaryBranch();
        // 校验
        if (branch.getIsExecuted() != null && branch.getIsExecuted()) {
            // 执行过了就不要再执行了。
            return;
        }
        // 切点
        ProceedingJoinPoint joinPoint = context.getJoinPoint();
        try {
            // 执行分支方法
            Object res = this.invokeSecondaryMethod(joinPoint);
            branch.setRes(res);
        } catch (Throwable e) {
            // 保存异常对象
            branch.setException(e);
        }
    }

    /**
     * 执行 比对 功能
     */
    @Override
    public void compare(JanusContext context) {
        BranchInfo primaryBranch = context.getPrimaryBranch();
        BranchInfo secondaryBranch = context.getSecondaryBranch();
        Map<String, String> compareResMap = JsonUtils.compareObj(primaryBranch.getRes(), secondaryBranch.getRes());
        context.setCompareResMap(compareResMap);
    }

    /**
     * 设置主分支和比对分支
     */
    private void setMasterBranch(JanusContext context) {
        String masterBranchName = context.getMasterBranchName();
        if (JanusConstants.PRIMARY.equals(masterBranchName)) {
            context.setMasterBranch(context.getPrimaryBranch());
            context.setCompareBranch(context.getSecondaryBranch());
        } else if (JanusConstants.SECONDARY.equals(masterBranchName)) {
            context.setMasterBranch(context.getSecondaryBranch());
            context.setCompareBranch(context.getPrimaryBranch());
        } else {
            throw new JanusException("不支持的 masterBranch 类型: [" + masterBranchName + "]");
        }
    }

    /**
     * 执行次要分支
     */
    private Object invokeSecondaryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        SecondaryMethodInfo secondaryServiceAndMethod = this.getSecondaryServiceAndMethod(joinPoint);
        Object secondaryService = secondaryServiceAndMethod.getSecondaryService();
        Method secondaryMethod = secondaryServiceAndMethod.getSecondaryMethod();

        // 执行 Secondary 方法
        return secondaryMethod.invoke(secondaryService, joinPoint.getArgs());
    }

    /**
     * 获取次要分支的 service 对象 和 方法对象
     */
    private SecondaryMethodInfo getSecondaryServiceAndMethod(ProceedingJoinPoint joinPoint) {
        /* 获取 service 对象 */
        // 获取 Primary Service
        Object primaryService = joinPoint.getTarget();
        Class<?> primaryClass = AopUtils.getTargetClass(primaryService);
        Method primaryMethod = null;
        // 从缓存中获取 secondaryService
        Object secondaryService = secondaryBeanCache.get(primaryClass);
        // 如果缓存中没有，则用反射获取 secondaryService
        if (secondaryService == null) {

            primaryMethod = this.getPrimaryMethod(joinPoint);
            // 修正：遍历实现类所有的接口，找到定义了该方法的接口
            Class<?> interfaceType = null;
            for (Class<?> iFace : primaryClass.getInterfaces()) {
                try {
                    iFace.getMethod(primaryMethod.getName(), primaryMethod.getParameterTypes());
                    interfaceType = iFace;
                    break;
                } catch (NoSuchMethodException e) {
                    // 当前接口没有该方法，继续查找
                }
            }
            if (interfaceType == null) {
                throw new JanusException(
                        String.format("[%s]未实现含有[%s]方法的接口", primaryClass.getSimpleName(), primaryMethod.getName())
                );
            }
            Map<String, ?> beans = applicationContext.getBeansOfType(interfaceType);
            for (Object service : beans.values()) {
                Class<?> beanClass = AopUtils.getTargetClass(service);
                // 找到带有 @Secondary 注解的 Service
                if (beanClass.isAnnotationPresent(Secondary.class)) {
                    secondaryService = service;
                    // 将 secondaryService 放入缓存中
                    secondaryBeanCache.put(primaryClass, service);
                    break;
                }
            }
        }

        if (secondaryService == null) {
            throw new JanusException(primaryClass.getSimpleName() + " 未找到 Secondary Service");
        }

        /* 获取 目标方法 */
        Method secondaryMethod;
        if (primaryMethod == null) {
            primaryMethod = this.getPrimaryMethod(joinPoint);
        }
        Method cached = secondaryMethodCache.get(primaryMethod);
        if (cached != null) {
            secondaryMethod = cached;
        } else {
            Class<?> secondaryClass = AopUtils.getTargetClass(secondaryService);
            // 获取同名同参数的方法
            try {
                secondaryMethod = secondaryClass.getMethod(primaryMethod.getName(), primaryMethod.getParameterTypes());
            } catch (NoSuchMethodException e) {
                throw new JanusException(
                        String.format("[%s.%s]未找到", secondaryClass.getSimpleName(), primaryMethod.getName()),
                        e
                );
            }
            secondaryMethodCache.put(primaryMethod, secondaryMethod);
        }

        return SecondaryMethodInfo.builder()
                .secondaryService(secondaryService)
                .secondaryMethod(secondaryMethod)
                .build();
    }

    /**
     * 根据切点，获取主分支方法对象
     *
     * @param joinPoint 切点
     * @return 主分支方法对象
     */
    private Method getPrimaryMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
}
