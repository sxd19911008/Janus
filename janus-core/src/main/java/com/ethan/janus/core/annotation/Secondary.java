package com.ethan.janus.core.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited // 可以被继承，让代理类可以继承此注解
@Documented // 指示该注解应被包含在 JavaDoc 中。 当你使用 javadoc 工具生成 API 文档时，这个注解也会显式地列出来。
public @interface Secondary {

}
