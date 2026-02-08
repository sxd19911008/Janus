package com.ethan.janus.core.dto;

import lombok.*;

/**
 * 分流比对框架 分支信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class BranchInfoImpl implements BranchInfo {

    // 分支类型: primary-主分支; secondary-次要分支
    private String branchType;

    // 是否已经执行过: true-执行过; false 或者 null，都表示未执行过
    @Setter
    private Boolean isExecuted;
    // 计算结果对象
    @Setter
    private Object res;
    // 异常对象
    @Setter
    private Throwable exception;
    /*
     * 是否做事务回滚：如果为true，Janus框架会强制添加事务并回滚分支执行的部分。
     * 1. 如果已经存在事务，会采用已经存在的事务。
     * 2. 不会回滚整个事务，仅回滚分支执行的部分。
     */
    @Setter
    private Boolean isRollback;
    // 是否是异步执行
    @Setter
    private Boolean isAsync;
}
