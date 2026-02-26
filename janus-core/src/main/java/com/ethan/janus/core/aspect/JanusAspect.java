package com.ethan.janus.core.aspect;

import com.ethan.janus.core.annotation.Janus;
import com.ethan.janus.core.compare.JanusCompare;
import com.ethan.janus.core.config.ExpressionRootObject;
import com.ethan.janus.core.config.JanusConfigProperties;
import com.ethan.janus.core.config.JanusExpressionEvaluator;
import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.core.constants.JanusConstants;
import com.ethan.janus.core.dto.BranchInfoImpl;
import com.ethan.janus.core.dto.JanusContextImpl;
import com.ethan.janus.core.dto.PluginListDTO;
import com.ethan.janus.core.exception.JanusException;
import com.ethan.janus.core.lifecycle.LifecycleDecoratorManager;
import com.ethan.janus.core.manager.JanusCompareManager;
import com.ethan.janus.core.manager.JanusPluginManager;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.core.utils.JanusLogUtils;
import com.ethan.janus.core.utils.JanusUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Janus 核心
 */
@Slf4j
// 不能设置最小值，设置最小值会报错：java.lang.IllegalStateException: Required to bind 2 arguments, but only bound 1 (JoinPointMatch was NOT bound in invocation)
@Order(Integer.MIN_VALUE + 1) // 设置最高优先级，让该切面最先执行
@Component
@Aspect
public class JanusAspect {

    @Autowired
    private LifecycleDecoratorManager lifecycleDecoratorManager;
    @Autowired
    private JanusPluginManager janusPluginManager;
    @Autowired
    private JanusCompareManager janusCompareManager;
    @Autowired
    private ExecutorService janusBranchThreadPool;
    @Autowired
    private ExecutorService janusCompareThreadPool;
    @Autowired
    private JanusConfigProperties janusConfigProperties;
    @Autowired
    private JanusExpressionEvaluator janusExpressionEvaluator;

    @Around("@annotation(janus)")
    public Object janusAspect(ProceedingJoinPoint joinPoint, Janus janus) throws Throwable {
        // 如果总开关关闭了，则直接执行切点方法并返回结果
        if (janusConfigProperties.isClosed()) {
            return joinPoint.proceed();
        }

        /* 组装分支信息 */
        BranchInfoImpl primaryBranch = BranchInfoImpl.builder()
                .branchType(JanusConstants.PRIMARY)
                .isExecuted(false)
                .isRollback(false)
                .build();
        BranchInfoImpl secondaryBranch = BranchInfoImpl.builder()
                .branchType(JanusConstants.SECONDARY)
                .isExecuted(false)
                .isRollback(false)
                .build();

        /* 插件 */
        // 添加插件
        PluginListDTO pluginListDTO = this.addPlugins(janus);
        // 高优先级插件，order 小于0
        List<JanusPlugin> higherPluginList = pluginListDTO.getHigherPluginList();
        // 低优先级插件，order 大于0
        List<JanusPlugin> lowerPluginList = pluginListDTO.getLowerPluginList();

        /* 比对功能具体实现 */
        JanusCompare janusCompare = janusCompareManager.getJanusCompare(janus.compareImpl());

        /* 业务数据键 */
        String businessKey = this.getBusinessKey(janus, joinPoint);

        /* 设置比对类型 */
        CompareType compareType;
        if (this.isCompareTypeNull(janus.compareType())) {
            compareType = CompareType.valueOf(janusConfigProperties.getDefaultCompareType());
        } else {
            compareType = janus.compareType();
        }

        /* 忽略字段 */
        Set<String> ignoreFieldPaths;
        String[] ignoreFieldPathsArr = janus.ignoreFieldPaths();
        if (ignoreFieldPathsArr == null || ignoreFieldPathsArr.length == 0) {
            ignoreFieldPaths = null;
        } else {
            ignoreFieldPaths = new HashSet<>(Arrays.asList(janus.ignoreFieldPaths()));
        }


        /* 创建上下文对象 */
        JanusContextImpl context = JanusContextImpl.builder()
                .joinPoint(joinPoint)
                .lifecycle(lifecycleDecoratorManager)
                .higherPluginList(higherPluginList)
                .lowerPluginList(lowerPluginList)
                .janusCompare(janusCompare)
                .methodId(janus.methodId())
                .businessKey(businessKey)
                .compareType(compareType)
                .isCompare(!(compareType == CompareType.DO_NOT_COMPARE))
                .isAsyncCompare(janus.isAsyncCompare())
                .primaryBranch(primaryBranch)
                .secondaryBranch(secondaryBranch)
                .pluginDataMap(new ConcurrentHashMap<>())
                .ignoreFieldPaths(ignoreFieldPaths)
                .build();

        this.logInfo(context, "Janus begin", "compareType", compareType.name());

        /* 分流 */
        context.getLifecycle().switchBranch(context);

        /* CompareType 为 ROLLBACK 的场景下，先执行比对分支代码 */
        try {
            if (CompareType.hasRollback(compareType)) {
                /* 执行比对分支代码 */
                this.compareBranchExecute(context);
            }
        } catch (Throwable e) {
            // 比对流程报错不影响主分支
            this.logError(context, "compareBranchExecute", e);
        }

        /* 执行主分支代码 */
        this.masterBranchExecute(context);

        /* 比对 */
        // 处理比对流程
        try {
            this.handleCompare(context);
        } catch (Throwable e) {
            // 比对流程报错不影响主分支
            this.logError(context, "handleCompare", e);
        }

        /* 返回结果 */
        if (context.getMasterBranch().getException() != null) {
            throw context.getMasterBranch().getException();
        } else {
            return context.getMasterBranch().getBranchRes().getRes();
        }
    }

    private boolean isCompareTypeNull(CompareType compareType) {
        return compareType == null || compareType == CompareType.NONE;
    }

    /**
     * 处理比对流程，需要判断是否比对，如何比对等问题，以及
     */
    private void handleCompare(JanusContextImpl context) {
        if (context.isNotCompare()) {
            // 不比对，直接返回
            return;
        }
        switch (context.getCompareType()) {
            // 异步比对
            case ASYNC_COMPARE:
                janusBranchThreadPool.execute(() -> this.compareTwoBranch(context));
                break;
            // 同步比对
            case SYNC_COMPARE:
            case SYNC_ROLLBACK_ONE_COMPARE:
            case SYNC_ROLLBACK_ALL_COMPARE:
                this.compareTwoBranch(context);
                break;
            // 不比对
            case DO_NOT_COMPARE:
                break;
            // 默认报错
            default:
                throw new JanusException("不支持的 compareType 类型: [" + context.getCompareType() + "]");
        }
    }

    /**
     * 比对，需要根据配置判断比对类型，比如异步比对、同步比对等。
     */
    private void compareTwoBranch(JanusContextImpl context) {
        /* 执行比对分支代码 */
        this.compareBranchExecute(context);

        /* 比对 */
        if (context.isAsyncCompare()) {
            janusCompareThreadPool.execute(() -> context.getLifecycle().compare(context));
        } else {
            // 同步比对
            context.getLifecycle().compare(context);
        }
    }

    /**
     * 执行主分支
     */
    private void masterBranchExecute(JanusContextImpl context) {
        context.masterBranchExecute();
    }

    /**
     * 执行比对分支
     */
    private void compareBranchExecute(JanusContextImpl context) {
        context.compareBranchExecute();
    }

    /**
     * 解析SpEL表达式，获取 业务数据键
     */
    private String getBusinessKey(Janus janus, ProceedingJoinPoint joinPoint) {
        // SpEL 表达式
        String businessKeySpEL = janus.businessKey();
        // 表达式为空直接返回
        if (JanusUtils.isBlank(businessKeySpEL)) {
            return "";
        }
        // 切点方法对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 切点所在的原始 bean
        Object target = joinPoint.getTarget();
        Class<?> targetClass = target.getClass();
        // 切点方法入参
        Object[] args = joinPoint.getArgs();

        // rootObject
        ExpressionRootObject rootObject = ExpressionRootObject.builder()
                .targetBean(target)
                .build();

        // 通过表达式获取 业务数据键
        Object evaluate = janusExpressionEvaluator.evaluate(businessKeySpEL, method, targetClass, args, rootObject);
        // 返回结果
        if (evaluate == null) {
            return "";
        } else {
            return String.valueOf(evaluate);
        }
    }

    /**
     * 添加 @Janus 中配置的插件
     */
    private PluginListDTO addPlugins(Janus janus) {
        /* 获取所有 janus 配置的插件 */
        List<JanusPlugin> pluginList = this.getPluginList(janus);
        /* 插件分类 */
        // 插件排序，越小的越优先，放在前面
        pluginList = pluginList.stream().sorted(Comparator.comparing(JanusPlugin::getOrder)).collect(Collectors.toList());
        // 高优先级插件，order 小于0
        List<JanusPlugin> higherPluginList = new ArrayList<>();
        // 低优先级插件，order 大于0
        List<JanusPlugin> lowerPluginList = new ArrayList<>();
        for (JanusPlugin janusPlugin : pluginList) {
            if (janusPlugin.getOrder() < 0) {
                higherPluginList.add(janusPlugin);
            } else {
                lowerPluginList.add(janusPlugin);
            }
        }
        /* 返回数据 */
        return PluginListDTO.builder()
                .higherPluginList(higherPluginList)
                .lowerPluginList(lowerPluginList)
                .build();
    }

    /**
     * 获取插件
     */
    private List<JanusPlugin> getPluginList(Janus janus) {
        // 查询法级别的插件
        List<JanusPlugin> methodPluginList = janusPluginManager.getMethodPluginList(janus.plugins());
        // 查询全局插件
        List<JanusPlugin> globalPluginList = janusPluginManager.getAllGlobalPluginList();
        // 合并
        methodPluginList.addAll(globalPluginList);
        return methodPluginList;
    }

    /**
     * 统一打印 Info 日志
     */
    private void logInfo(JanusContextImpl context, String lifecycle, String key, Object value) {
        if (log.isInfoEnabled()) {
            log.info(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:{}] >> {}={}",
                    JanusLogUtils.SUCCESS_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    lifecycle,
                    key,
                    JanusLogUtils.toJsonString(value)
            );
        }
    }

    /**
     * 统一打印 Error 日志
     */
    private void logError(JanusContextImpl context, String lifecycle, Throwable e) {
        log.error(
                "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:{}] >> exception=",
                JanusLogUtils.FAIL_ICON,
                context.getMethodId(),
                context.getBusinessKey(),
                lifecycle,
                e
        );
    }
}
