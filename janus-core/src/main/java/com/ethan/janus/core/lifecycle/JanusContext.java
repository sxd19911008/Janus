package com.ethan.janus.core.lifecycle;

import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.core.dto.BranchInfo;
import com.ethan.janus.core.exception.JanusException;
import lombok.*;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Map;

/**
 * Janus 对外暴露的上下文
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JanusContext {

    // 切点对象
    private ProceedingJoinPoint joinPoint;

    // 生命周期实现
    private JanusLifecycle lifecycle;

    // 比对类型
    @Getter
    private CompareType compareType;

    // 主分支，只允许设置1次，不能随意修改该属性
    @Getter
    private String masterBranchName;

    // 加了 Janus 注解的分支
    @Getter
    private BranchInfo primaryBranch;
    // 次要分支
    @Getter
    private BranchInfo secondaryBranch;

    // 主分支
    @Getter
    private BranchInfo masterBranch;
    // 用于比对的分支
    @Getter
    private BranchInfo compareBranch;

    // 比对结果
    @Getter
    private Map<String, String> compareResMap;

    // 自定义数据
    private Map<Class<?>, Object> pluginDataMap;

    ProceedingJoinPoint getJoinPoint() {
        return joinPoint;
    }

    void setCompareType(CompareType compareType) {
        this.compareType = compareType;
    }

    public void setMasterBranchName(String masterBranchName) {
        // 只允许设置1次，不能随意修改该属性
        if (this.masterBranchName == null) {
            this.masterBranchName = masterBranchName;
        } else {
            throw new JanusException("masterBranchName 只能设置1次");
        }
    }

    void setMasterBranch(BranchInfo masterBranch) {
        this.masterBranch = masterBranch;
    }

    void setCompareBranch(BranchInfo compareBranch) {
        this.compareBranch = compareBranch;
    }

    void setCompareResMap(Map<String, String> compareResMap) {
        this.compareResMap = compareResMap;
    }

    JanusLifecycle getLifecycle() {
        return this.lifecycle;
    }

    Object getPluginData(Class<?> clazz) {
        return this.pluginDataMap.get(clazz);
    }

    void putPluginData(Class<?> clazz, Object data) {
        this.pluginDataMap.put(clazz, data);
    }
}
