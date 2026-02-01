package com.ethan.janus.core.annotation;

import com.ethan.janus.core.lifecycle.JanusPlugin;
import com.ethan.janus.core.constants.CompareType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited // 可以被继承，让代理类可以继承此注解
@Documented // 指示该注解应被包含在 JavaDoc 中。 当你使用 javadoc 工具生成 API 文档时，这个注解也会显式地列出来。
public @interface Janus {

    CompareType compareType() default CompareType.NONE;

    Class<? extends JanusPlugin>[] plugins() default {};
}
