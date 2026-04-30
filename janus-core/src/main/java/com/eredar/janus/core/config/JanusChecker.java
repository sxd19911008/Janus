package com.eredar.janus.core.config;

import com.eredar.janus.core.annotation.Janus;
import com.eredar.janus.core.constants.JanusCompareType;
import com.eredar.janus.core.exception.JanusException;
import com.eredar.janus.core.rollback.JanusRollbackClearCache;
import com.eredar.janus.core.utils.JanusAopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 校验 @Janus 注解中的配置是否合法
 * <p>1. methodId 是否重复
 * <p>2. 存在事务相关的比对类型(JanusCompareType)，必须实现`JanusRollbackClearCache`
 */
public class JanusChecker implements SmartInitializingSingleton {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JanusConfigProperties janusConfigProperties;
    @Autowired(required = false)
    private JanusRollbackClearCache janusRollbackClearCache;

    @Override
    public void afterSingletonsInstantiated() {
        // 如果开关关闭，则跳过校验
        if (!janusConfigProperties.getIsMethodIdDuplicateCheck()) {
            return;
        }

        // 存储 methodId
        Set<String> methodIdSet = new HashSet<>();
        // 记录重复的 methodId
        Set<String> duplicateSet = new HashSet<>();

        /* 循环统计重复的 methodId */
        final boolean[] hasRollback = {false}; // 是否存在事务相关的比对类型
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            // 获取 Bean 的原始类型（处理代理类）
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanType = JanusAopUtils.getProxyTargetClass(bean);

            // 扫描类中的所有方法
            ReflectionUtils.doWithMethods(beanType, method -> {
                // 查找你的自定义注解
                Janus annotation = AnnotationUtils.findAnnotation(method, Janus.class);

                if (annotation != null) {
                    /* methodId 是否重复 */
                    String methodId = annotation.methodId();
                    if (methodIdSet.contains(methodId)) {
                        // 记录重复 methodId
                        duplicateSet.add(methodId);
                    } else {
                        // methodId 不重复，保存起来用于后面的校验
                        methodIdSet.add(methodId);
                    }

                    /* 是否存在事务相关的比对类型 */
                    String compareType = annotation.compareType();
                    if (!hasRollback[0] && JanusCompareType.hasRollback(compareType)) {
                        hasRollback[0] = true;
                    }
                }
            });
        }

        /* 判断是否存在不合法的情况 */
        StringBuilder sb = new StringBuilder();

        if (hasRollback[0] && janusRollbackClearCache == null) {
            this.initStringBuilder(sb);
            sb.append("存在事务相关的比对类型，请提供 JanusRollbackClearCache 实现")
                    .append("\n");
        }

        if (!duplicateSet.isEmpty()) {
            this.initStringBuilder(sb);
            sb.append("@Janus 注解中的 methodId 重复：")
                    .append(duplicateSet)
                    .append("\n");
        }

        /* 抛出异常 */
        if (sb.length() > 0) {
            throw new JanusException(sb.toString());
        }
    }

    private void initStringBuilder(StringBuilder sb) {
        if (sb.length() == 0) {
            sb.append("@Janus 注解配置不合法")
                    .append("\n");
        }
    }
}
