package com.eredar.janus.core.aspect;

import com.eredar.janus.core.annotation.Janus;
import com.eredar.janus.core.compare.JanusCompare;
import com.eredar.janus.core.config.ExpressionRootObject;
import com.eredar.janus.core.config.JanusConfigProperties;
import com.eredar.janus.core.config.JanusExpressionEvaluator;
import com.eredar.janus.core.constants.JanusCompareType;
import com.eredar.janus.core.constants.JanusConstants;
import com.eredar.janus.core.dto.BranchInfoImpl;
import com.eredar.janus.core.dto.JanusAspectStatus;
import com.eredar.janus.core.dto.JanusContextImpl;
import com.eredar.janus.core.dto.PluginListDTO;
import com.eredar.janus.core.flow.JanusFlow;
import com.eredar.janus.core.lifecycle.LifecycleDecoratorManager;
import com.eredar.janus.core.manager.JanusCompareManager;
import com.eredar.janus.core.manager.JanusFlowManager;
import com.eredar.janus.core.manager.JanusPluginManager;
import com.eredar.janus.core.plugin.JanusPlugin;
import com.eredar.janus.core.utils.JanusLogUtils;
import com.eredar.janus.core.utils.JanusUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;

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
@Order(1) // 设置较高优先级，尽量让该切面先执行
@Aspect
public class JanusAspect {

    @Autowired
    private LifecycleDecoratorManager lifecycleDecoratorManager;
    @Autowired
    private JanusPluginManager janusPluginManager;
    @Autowired
    private JanusCompareManager janusCompareManager;
    @Qualifier("janusCompareThreadPool")
    @Autowired
    private ExecutorService janusCompareThreadPool;
    @Autowired
    private JanusConfigProperties janusConfigProperties;
    @Autowired
    private JanusExpressionEvaluator janusExpressionEvaluator;
    @Autowired
    private JanusFlowManager janusFlowManager;

    // 缓存忽略字段
    private final Map<Method, Set<String>> ignoreFieldPathsMap = new ConcurrentHashMap<>();

    @Around("@annotation(janus)")
    public Object janusAspect(ProceedingJoinPoint joinPoint, Janus janus) throws Throwable {
        boolean isFirst = false;
        try {
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

            /* 切点方法对象 */
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

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
            String businessKey = this.getBusinessKey(janus, joinPoint, method);

            /* 设置比对类型 */
            String compareType;
            if (this.isCompareTypeNull(janus.compareType())) {
                compareType = janusConfigProperties.getDefaultCompareType();
            } else {
                compareType = janus.compareType();
            }

            /* 忽略字段 */
            Set<String> ignoreFieldPaths;
            String[] ignoreFieldPathsArr = janus.ignoreFieldPaths();
            if (ignoreFieldPathsArr == null || ignoreFieldPathsArr.length == 0) {
                ignoreFieldPaths = null;
            } else {
                ignoreFieldPaths = ignoreFieldPathsMap.computeIfAbsent(method, k -> new HashSet<>(Arrays.asList(ignoreFieldPathsArr)));
            }

            /* 创建上下文对象 */
            JanusContextImpl context = JanusContextImpl.builder()
                    .joinPoint(joinPoint)
                    .method(method)
                    .lifecycle(lifecycleDecoratorManager)
                    .janusCompareThreadPool(janusCompareThreadPool)
                    .higherPluginList(higherPluginList)
                    .lowerPluginList(lowerPluginList)
                    .janusCompare(janusCompare)
                    .methodId(janus.methodId())
                    .businessKey(businessKey)
                    .compareType(compareType)
                    .needCompare(JanusCompareType.needCompare(compareType))
                    .isAsyncCompare(janus.isAsyncCompare())
                    .primaryBranch(primaryBranch)
                    .secondaryBranch(secondaryBranch)
                    .pluginDataMap(new ConcurrentHashMap<>())
                    .ignoreFieldPaths(ignoreFieldPaths)
                    .build();

            log.debug(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:Janus begin] >> compareType={}",
                    JanusLogUtils.SUCCESS_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    compareType
            );

            /* 分流 */
            context.getLifecycle().switchBranch(context);

            /* JanusAspectSupport 初始化 */
            // 只有当前线程第一次进入 JanusAspect，才会初始化 JanusAspectSupport
            isFirst = JanusAspectSupport.init();
            JanusAspectSupport.putStatus(
                    context.getMethodId(),
                    JanusAspectStatus.builder()
                            .masterBranchName(context.getMasterBranchName())
                            .build()
            );

            /* 执行所有分支并比对结果 */
            // 根据比对类型获取具体流程
            JanusFlow flow = janusFlowManager.getFlow(compareType);
            // 执行流程
            flow.execute(context);

            /* 返回结果 */
            if (context.getMasterBranch().getException() != null) {
                throw context.getMasterBranch().getException();
            } else {
                return context.getMasterBranch().getBranchRes().getRes();
            }
        } finally {
            // 第一次进入 JanusAspect 时，必须在重置 isFirst属性
            if (isFirst) {
                JanusAspectSupport.resetIsFirst();
            }
        }
    }

    private boolean isCompareTypeNull(String compareType) {
        return JanusUtils.isBlank(compareType) || JanusCompareType.NONE.equals(compareType);
    }

    /**
     * 解析SpEL表达式，获取 业务数据键
     */
    private String getBusinessKey(Janus janus, ProceedingJoinPoint joinPoint, Method method) {
        // SpEL 表达式
        String businessKeySpEL = janus.businessKey();
        // 表达式为空直接返回
        if (JanusUtils.isBlank(businessKeySpEL)) {
            return "";
        }

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
}
