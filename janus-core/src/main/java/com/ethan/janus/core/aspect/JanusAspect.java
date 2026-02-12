package com.ethan.janus.core.aspect;

import com.ethan.janus.core.annotation.Janus;
import com.ethan.janus.core.config.ExpressionRootObject;
import com.ethan.janus.core.config.JanusConfigProperties;
import com.ethan.janus.core.config.JanusExpressionEvaluator;
import com.ethan.janus.core.config.JanusPluginManager;
import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.core.constants.JanusConstants;
import com.ethan.janus.core.dto.BranchInfoImpl;
import com.ethan.janus.core.dto.JanusContextImpl;
import com.ethan.janus.core.dto.PluginListDTO;
import com.ethan.janus.core.exception.JanusException;
import com.ethan.janus.core.lifecycle.LifecycleDecoratorManager;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.core.utils.JanusUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Janus 核心
 */
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

        /* 业务数据键 */
        String businessKey = this.getBusinessKey(janus, joinPoint);

        /* 创建上下文对象 */
        JanusContextImpl context = JanusContextImpl.builder()
                .joinPoint(joinPoint)
                .lifecycle(lifecycleDecoratorManager)
                .higherPluginList(higherPluginList)
                .lowerPluginList(lowerPluginList)
                .methodId(janus.methodId())
                .businessKey(businessKey)
                .isAsyncCompare(janus.isAsyncCompare())
                .primaryBranch(primaryBranch)
                .secondaryBranch(secondaryBranch)
                .pluginDataMap(new ConcurrentHashMap<>())
                .build();

        /* 设置比对类型 */
        if (this.isCompareTypeNull(janus.compareType())) {
            context.setCompareType(CompareType.valueOf(janusConfigProperties.getDefaultCompareType()));
        } else {
            context.setCompareType(janus.compareType());
        }

        /* 分流 */
        context.getLifecycle().switchBranch(context);

        /* 执行主分支代码 */
        this.executedMasterBranch(context);

        /* 比对 */
        // 处理比对流程
        try {
            this.handleCompare(context);
        } catch (Throwable e) {
            // 比对流程报错不影响主分支
            // TODO 日志框架
            e.printStackTrace();
        }

        /* 返回结果 */
        if (context.getMasterBranch().getException() != null) {
            throw context.getMasterBranch().getException();
        } else {
            return context.getMasterBranch().getRes();
        }
    }

    private boolean isCompareTypeNull(CompareType compareType) {
        return compareType == null || compareType == CompareType.NONE;
    }

    /**
     * 处理比对流程，需要判断是否比对，如何比对等问题，以及
     */
    private void handleCompare(JanusContextImpl context) {
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
        this.executedCompareBranch(context);

        /* 比对 */
        if (context.getIsAsyncCompare()) {
            janusCompareThreadPool.execute(() -> context.getLifecycle().compare(context));
        } else {
            // 同步比对
            context.getLifecycle().compare(context);
        }
    }

    /**
     * 执行主分支代码
     */
    private void executedMasterBranch(JanusContextImpl context) {
        if (JanusConstants.PRIMARY.equals(context.getMasterBranchName())) {
            // 执行切点方法
            context.getLifecycle().primaryExecute(context);
        } else if (JanusConstants.SECONDARY.equals(context.getMasterBranchName())) {
            // 执行次要分支
            context.getLifecycle().secondaryExecute(context);
        } else {
            throw new JanusException("不支持的 MasterBranch 类型: [" + context.getMasterBranchName() + "]");
        }
    }

    /**
     * 执行比对分支代码
     */
    private void executedCompareBranch(JanusContextImpl context) {
        if (JanusConstants.PRIMARY.equals(context.getMasterBranchName())) {
            // 由于主分支是 primary，所以这里执行 secondary 分支
            context.getLifecycle().secondaryExecute(context);
        } else if (JanusConstants.SECONDARY.equals(context.getMasterBranchName())) {
            // 由于主分支是 secondary，所以这里执行 primary 分支
            context.getLifecycle().primaryExecute(context);
        } else {
            throw new JanusException("不支持的 MasterBranch 类型: [" + context.getMasterBranchName() + "]");
        }
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
}
