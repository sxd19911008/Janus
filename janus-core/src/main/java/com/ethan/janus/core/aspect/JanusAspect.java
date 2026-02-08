package com.ethan.janus.core.aspect;

import com.ethan.janus.core.annotation.Janus;
import com.ethan.janus.core.config.JanusConfigProperties;
import com.ethan.janus.core.config.JanusPluginManager;
import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.core.constants.JanusConstants;
import com.ethan.janus.core.dto.BranchInfoImpl;
import com.ethan.janus.core.dto.JanusContextImpl;
import com.ethan.janus.core.exception.JanusException;
import com.ethan.janus.core.lifecycle.LifecycleDecoratorManager;
import com.ethan.janus.core.plugin.JanusPlugin;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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
    private JanusConfigProperties janusConfigProperties;

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
        List<JanusPlugin> pluginList = this.getPluginList(janus);
        // 插件排序，越小的越优先，放在前面
        pluginList = pluginList.stream().sorted(Comparator.comparing(JanusPlugin::getOrder)).collect(Collectors.toList());
        // 高优先级插件，order 小于0
        List<JanusPlugin> higherPluginList = new ArrayList<>();
        // 低优先级插件，order 大于0
        List<JanusPlugin> lowerPluginList = new ArrayList<>();
        // 插件分类
        for (JanusPlugin janusPlugin : pluginList) {
            if (janusPlugin.getOrder() < 0) {
                higherPluginList.add(janusPlugin);
            } else {
                lowerPluginList.add(janusPlugin);
            }
        }

        /* 创建上下文对象（循环依赖结构） */
        JanusContextImpl context = JanusContextImpl.builder()
                .joinPoint(joinPoint)
                .lifecycle(lifecycleDecoratorManager)
                .higherPluginList(higherPluginList)
                .lowerPluginList(lowerPluginList)
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
        this.handleCompare(context);

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
        context.getLifecycle().compare(context);
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
     * 获取插件
     */
    private List<JanusPlugin> getPluginList(Janus janus) {
        // 查询插件 class 类型
        Class<? extends JanusPlugin>[] pluginClassArr = janus.plugins();
        // 查询插件单例对象
        return janusPluginManager.getJanusPluginList(pluginClassArr);
    }
}
