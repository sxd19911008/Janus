package com.ethan.janus.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * 分流比对框架 次要方法信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SecondaryMethodInfo {
    // 目标对象
    private Object secondaryService;
    // 目标方法
    private Method secondaryMethod;
}
