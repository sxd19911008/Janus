package com.eredar.janus.core.annotation;

import com.eredar.janus.core.compare.JanusCompareDefaultImpl;
import com.eredar.janus.core.compare.JanusCompare;
import com.eredar.janus.core.constants.JanusCompareType;
import com.eredar.janus.core.plugin.JanusPlugin;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited // 可以被继承，让代理类可以继承此注解
@Documented // 指示该注解应被包含在 JavaDoc 中。 当你使用 javadoc 工具生成 API 文档时，这个注解也会显式地列出来。
public @interface Janus {

    // 方法唯一标识
    String methodId();

    // 比对类型
    String compareType() default JanusCompareType.NONE;

    // 业务数据键
    String businessKey() default "";

    /*
     * 比对分支运行完后，比对2个分支的结果的过程是否异步执行。
     * 可配置项：
     *      1.true: 异步执行比对功能
     *      2.false: 主线程同步执行比对功能
     * 不配置该项时，默认为 true，表示异步执行比对过程。
     */
    boolean isAsyncCompare() default true;

    // 比对功能实现类
    Class<? extends JanusCompare> compareImpl() default JanusCompareDefaultImpl.class;

    // 插件数组
    Class<? extends JanusPlugin>[] plugins() default {};

    /**
     * 比对时忽略的字段路径列表
     * 例如：{"id", "createTime", "user.password"}
     */
    String[] ignoreFieldPaths() default {};
}
