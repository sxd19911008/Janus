package com.ethan.janus.core.lifecycle;

import com.ethan.janus.core.annotation.Janus;
import com.ethan.janus.core.config.JanusConfig;
import com.ethan.janus.core.config.JanusPluginManager;
import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.core.constants.JanusConstants;
import com.ethan.janus.core.dto.BranchInfo;
import com.ethan.janus.core.exception.JanusException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Component
@Aspect
public class JanusAspect {

    @Autowired
    private JanusCoreLifecycle janusCoreLifecycle;
    @Autowired
    private JanusPluginManager janusPluginManager;
    @Autowired
    private ExecutorService janusThreadPool;
    @Autowired
    private JanusConfig janusConfig;

    @Around("@annotation(janus)")
    public Object janusAspect(ProceedingJoinPoint joinPoint, Janus janus) throws Throwable {
        // 如果总开关关闭了，则直接执行切点方法并返回结果
        if (janusConfig.isClosed()) {
            return joinPoint.proceed();
        }

        /* 组装分支信息 */
        BranchInfo primaryBranch = BranchInfo.builder()
                .branchType(JanusConstants.PRIMARY)
                .build();
        BranchInfo secondaryBranch = BranchInfo.builder()
                .branchType(JanusConstants.SECONDARY)
                .build();

        /* 插件 */
        List<JanusPlugin> pluginList = this.getPluginList(janus);
        CoreLifecycleProxy lifecycle = new CoreLifecycleProxy(janusCoreLifecycle, pluginList);

        /* 创建上下文对象（循环依赖结构） */
        JanusContext context = JanusContext.builder()
                .joinPoint(joinPoint)
                .lifecycle(lifecycle)
                .primaryBranch(primaryBranch)
                .secondaryBranch(secondaryBranch)
                .pluginDataMap(new HashMap<>())
                .build();

        /* 设置比对类型 */
        if (this.isCompareTypeNull(janus.compareType())) {
            context.setCompareType(CompareType.valueOf(janusConfig.getDefaultCompareType()));
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
    private void handleCompare(JanusContext context) {
        switch (context.getCompareType()) {
            // 异步比对
            case ASYNC_COMPARE:
                janusThreadPool.execute(() -> this.compareTwoBranch(context));
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
    private void compareTwoBranch(JanusContext context) {
        /* 执行比对分支代码 */
        BranchInfo compareBranch = context.getCompareBranch();
        // 如果是异步执行，需要设置相关标识
        if (context.getCompareType() == CompareType.ASYNC_COMPARE) {
            compareBranch.setIsAsync(true);
        }
        this.executedCompareBranch(context);

        /* 比对 */
        context.getLifecycle().compare(context);
    }

    /**
     * 执行主分支代码
     */
    private void executedMasterBranch(JanusContext context) {
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
    private void executedCompareBranch(JanusContext context) {
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
