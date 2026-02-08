package com.ethan.janus.core.config;

import com.ethan.janus.core.annotation.Janus;
import com.ethan.janus.core.exception.JanusException;
import com.ethan.janus.core.utils.JanusAopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 校验 @Janus 注解中的 methodId 是否重复
 */
@Component
public class JanusMethodIdDuplicateChecker implements SmartInitializingSingleton {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JanusConfigProperties janusConfigProperties;

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
                    String methodId = annotation.methodId();
                    if (methodIdSet.contains(methodId)) {
                        // 记录重复 methodId
                        duplicateSet.add(methodId);
                    } else {
                        // methodId 不重复，保存起来用于后面的校验
                        methodIdSet.add(methodId);
                    }
                }
            });
        }

        /* 抛出异常 */
        if (!duplicateSet.isEmpty()) {
            throw new JanusException("@Janus 注解中的 methodId 重复：" + duplicateSet);
        }
    }
}
