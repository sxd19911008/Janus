package com.eredar.janus.core.config;

import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Janus SpEL 表达式求值器。
 */
public class JanusExpressionEvaluator extends CachedExpressionEvaluator {

    /**
     * Expression 缓存容器。
     * key = ExpressionKey（由 AnnotatedElementKey + 表达式字符串 组合而成）
     */
    private final Map<ExpressionKey, Expression> expressionCache = new ConcurrentHashMap<>(2048);

    /**
     * 解析 SpEL 表达式，从方法入参中提取唯一键
     *
     * @param expression  SpEL 表达式字符串
     * @param method      被注解标记的方法
     * @param targetClass 目标对象的实际类型（用于缓存 key 区分不同类的同名方法）
     * @param args        方法实际入参值
     * @param rootObject  上下文使用的 rootObject
     * @return 解析后的唯一键字符串
     */
    public Object evaluate(String expression, Method method, Class<?> targetClass, Object[] args, Object rootObject) {
        // 构造缓存 key：方法 + 目标类
        AnnotatedElementKey elementKey = new AnnotatedElementKey(method, targetClass);

        // 从缓存获取或解析 Expression（父类方法，自动处理缓存）
        Expression expr = this.getExpression(this.expressionCache, elementKey, expression);

        // 构建求值上下文，自动将方法参数注册为 #paramName 变量
        EvaluationContext context = new MethodBasedEvaluationContext(
                rootObject,
                method, // 方法对象
                args, // 实际入参
                this.getParameterNameDiscoverer() // 父类内置的参数名发现器
        );

        // 求值并返回
        return expr.getValue(context);
    }
}