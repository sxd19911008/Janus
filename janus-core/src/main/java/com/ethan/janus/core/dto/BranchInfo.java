package com.ethan.janus.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分流比对框架 分支信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BranchInfo {

    // 分支类型: primary-主分支; secondary-次要分支
    private String branchType;
    // 是否已经执行过: true-执行过; false 或者 null，都表示未执行过
    private Boolean isExecuted;
    // 计算结果对象
    private Object res;
    // 异常对象
    private Throwable exception;
    // 是否是异步执行
    private Boolean isAsync;
}
