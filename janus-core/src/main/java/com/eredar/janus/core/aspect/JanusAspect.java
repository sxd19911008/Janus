package com.eredar.janus.core.aspect;

import com.eredar.janus.core.annotation.Janus;
import com.eredar.janus.core.compare.JanusCompare;
import com.eredar.janus.core.config.ExpressionRootObject;
import com.eredar.janus.core.config.JanusConfigProperties;
import com.eredar.janus.core.config.JanusExpressionEvaluator;
import com.eredar.janus.core.constants.CompareType;
import com.eredar.janus.core.constants.JanusConstants;
import com.eredar.janus.core.dto.BranchInfoImpl;
import com.eredar.janus.core.dto.JanusAspectStatus;
import com.eredar.janus.core.dto.JanusContextImpl;
import com.eredar.janus.core.dto.PluginListDTO;
import com.eredar.janus.core.exception.JanusException;
import com.eredar.janus.core.lifecycle.LifecycleDecoratorManager;
import com.eredar.janus.core.manager.JanusCompareManager;
import com.eredar.janus.core.manager.JanusPluginManager;
import com.eredar.janus.core.plugin.JanusPlugin;
import com.eredar.janus.core.threadpool.JanusBranchThreadPoolMetricsProvider;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
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
    @Qualifier("janusBranchThreadPool")
    @Autowired
    private ExecutorService janusBranchThreadPool;
    @Qualifier("janusCompareThreadPool")
    @Autowired
    private ExecutorService janusCompareThreadPool;
    @Autowired
    private JanusConfigProperties janusConfigProperties;
    @Autowired
    private JanusExpressionEvaluator janusExpressionEvaluator;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JanusBranchThreadPoolMetricsProvider janusBranchThreadPoolMetricsProvider;

    // 缓存忽略字段
    private final Map<Method, Set<String>> ignoreFieldPathsMap = new ConcurrentHashMap<>();
    // 统计当前时间点，每个异步执行compareBranch的方法的流量。每次进入本切面，数字加1；异步比对完成后，数字减1。
    private final Map<Method, AtomicInteger> methodCountMap = new ConcurrentHashMap<>();

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
                ignoreFieldPaths = ignoreFieldPathsMap.computeIfAbsent(method, k -> new HashSet<>(Arrays.asList(ignoreFieldPathsArr)));
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

            log.debug(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:Janus begin] >> compareType={}",
                    JanusLogUtils.SUCCESS_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    compareType.name()
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

            /* 统计当前方法的流量 */
            this.incrementMethodCountMap(context, method);

            /* 根据场景，在主线程中，选择分支代码执行 */
            if (CompareType.hasRollback(compareType)) {
                // 开启一个总事务，让2个分支在一个事务中，尽量在事务层面保持数据一致
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        try {
                            // CompareType 为 ROLLBACK 的场景下，先执行比对分支代码
                            compareBranchExecute(context);
                            // 执行主分支代码
                            masterBranchExecute(context);
                        } catch (Throwable e) {
                            /*
                             * 必定抛出 RuntimeException，让事务可以回滚。
                             * 由于分支运行时已经catch了异常，所以这里一般走不进来，仅作为兜底逻辑
                             */
                            if (e instanceof RuntimeException) {
                                throw e;
                            } else {
                                throw new RuntimeException(e);
                            }
                        } finally {
                            // 同步 rollback-only 状态
                            if (status.isRollbackOnly()) {
                                status.setRollbackOnly();
                            }
                        }
                    }
                });
            } else {
                // 没有 ROLLBACK 场景，仅运行主分支代码
                this.masterBranchExecute(context);
            }

            /* 比对 */
            // 处理比对流程
            this.handleCompare(context, method);

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

    private boolean isCompareTypeNull(CompareType compareType) {
        return compareType == null || compareType == CompareType.NONE;
    }

    /**
     * 处理比对流程，需要判断是否比对，如何比对等问题，以及
     */
    private void handleCompare(JanusContextImpl context, Method method) {
        try {
            // 如果不比对，则直接返回
            if (context.isNotCompare()) {
                return;
            }

            switch (context.getCompareType()) {
                // 异步比对
                case ASYNC_COMPARE:
                    // 如果当前方法已经积压太多的比对任务，则丢弃当前的比对任务
                    if (this.shouldThrottle(method)) {
                        // 计数器减一
                        this.decrementMethodCountMap(context, method);
                        return;
                    }
                    // 异步执行 compareBranch，然后比对2个分支的结果
                    janusBranchThreadPool.execute(() -> this.executeCompareBranchThenCompare(context, method));
                    break;
                // 同步比对
                case SYNC_COMPARE:
                    this.executeCompareBranchThenCompare(context);
                    break;
                case SYNC_ROLLBACK_ONE_COMPARE:
                case SYNC_ROLLBACK_ALL_COMPARE:
                    // 该场景下，compareBranch 已经执行完，所以直进行比对
                    this.compare(context);
                    break;
                // 不比对
                case DO_NOT_COMPARE:
                    break;
                // 默认报错
                default:
                    throw new JanusException("不支持的 compareType 类型: [" + context.getCompareType() + "]");
            }
        } catch (Throwable e) {
            // 比对流程报错不影响主分支
            log.error(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:handleCompare] >> exception=",
                    JanusLogUtils.FAIL_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    e
            );
        }
    }

    /**
     * 先执行比对分支代码，然后比对2个分支的结果
     */
    private void executeCompareBranchThenCompare(JanusContextImpl context) {
        /* 执行比对分支代码 */
        this.compareBranchExecute(context);
        /* 比对2个分支的结果 */
        this.compare(context);
    }

    /**
     * 先执行比对分支代码，然后比对2个分支的结果。执行完后，计数器减1
     */
    private void executeCompareBranchThenCompare(JanusContextImpl context, Method method) {
        try {
            /* 执行比对分支代码 */
            this.executeCompareBranchThenCompare(context);
        } finally {
            // 计数器减一
            this.decrementMethodCountMap(context, method);
        }
    }

    /**
     * 比对2个分支的结果
     * <p>需要根据配置判断比对类型，比如异步比对、同步比对等。
     */
    private void compare(JanusContextImpl context) {
        /* 比对 */
        if (context.isAsyncCompare()) {
            // 比对过程异步执行
            janusCompareThreadPool.execute(() -> context.getLifecycle().compare(context));
        } else {
            // 比对过程在主线程中同步执行
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
        try {
            // 执行比对分支代码
            context.compareBranchExecute();
        } catch (Throwable e) {
            // 比对流程报错不影响主分支
            log.error(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:compareBranchExecute] >> exception=",
                    JanusLogUtils.FAIL_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    e
            );
        }
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


    /**
     * 流量计数器加 1
     *
     * @param context 上下文
     * @param method  切点方法
     */
    private void incrementMethodCountMap(JanusContextImpl context, Method method) {
        try {
            // 校验开关
            if (janusConfigProperties.getAsyncCompareThrottling().isClosed()) {
                return;
            }
            // 异步执行比对分支，且需要比对时，才操作计数器
            if (CompareType.isAsyncCompareBranch(context.getCompareType()) && context.isCompare()) {
                // ConcurrentHashMap 配合 computeIfAbsent 可以保证线程安全
                AtomicInteger atomicInteger = methodCountMap.computeIfAbsent(method, k -> new AtomicInteger(0));
                // 计数器加 1
                atomicInteger.incrementAndGet();
            }
        } catch (Throwable e) {
            // 统计报错不影响主分支
            log.error(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:incrementMethodCountMap] >> exception=",
                    JanusLogUtils.FAIL_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    e
            );
        }
    }

    /**
     * 流量计数器减 1
     *
     * @param context 上下文
     * @param method  切点方法
     */
    private void decrementMethodCountMap(JanusContextImpl context, Method method) {
        try {
            // 校验开关
            if (janusConfigProperties.getAsyncCompareThrottling().isClosed()) {
                return;
            }
            AtomicInteger atomicInteger = methodCountMap.get(method);
            // 计数器减 1
            atomicInteger.decrementAndGet();
        } catch (Throwable e) {
            // 统计报错不影响主分支
            log.error(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:decrementMethodCountMap] >> exception=",
                    JanusLogUtils.FAIL_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    e
            );
        }
    }

    /**
     * 是否应该限流
     */
    private boolean shouldThrottle(Method method) {
        // 校验开关
        if (janusConfigProperties.getAsyncCompareThrottling().isClosed()) {
            return false;
        }

        // 只有线程池有压力时才进行限流判定，保证性能且不干扰正常扩容
        if (!this.isHighPressure()) {
            return false;
        }

        // 当前方法的流量
        int currentMethodCount;
        AtomicInteger atomicInteger = methodCountMap.get(method);
        if (atomicInteger == null) {
            currentMethodCount = 0;
        } else {
            currentMethodCount = atomicInteger.get();
        }

        // 流量太少，不需要做复杂判断，直接进行比对
        if (currentMethodCount < 10) {
            return false;
        }

        Collection<AtomicInteger> values = methodCountMap.values();
        int totalCount = values.stream().mapToInt(AtomicInteger::get).sum(); // 当前总数量
        int activeMethodsNum = values.size(); // 当前切面处理过的异步执行compareBranch的总方法数
        int average = totalCount / activeMethodsNum; // 每个方法的平均流量

        /*
         * 如果当前方法并发数等于或超过平均值，则触发限流。
         * 必须是大于等于，缺少等于会导致以下场景判断失效：
         * 1. 只有一个接口的场景。
         * 2. 所有接口的流量都完全一样的极端场景。
         */
        return currentMethodCount >= average;
    }

    /**
     * 判断线程池是否处于高压状态
     */
    private boolean isHighPressure() {
        // 队列当前 size
        int currentSize = janusBranchThreadPoolMetricsProvider.getQueueSize(janusBranchThreadPool);
        // 队列总 size
        int capacity = janusBranchThreadPoolMetricsProvider.getQueueCapacity(janusBranchThreadPool);
        // 占用比例
        double usageRatio = (double) currentSize / capacity;
        // 阈值默认 0.8
        return usageRatio > janusConfigProperties.getAsyncCompareThrottling().getLimitRatio();
    }
}
