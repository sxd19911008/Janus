package com.ethan.janus.core.dto;

import com.ethan.janus.core.compare.JanusCompare;
import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.core.exception.JanusException;
import com.ethan.janus.core.lifecycle.Lifecycle;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.core.utils.JanusUtils;
import lombok.*;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Janus 对外暴露的上下文
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JanusContextImpl implements JanusContext {

    // 切点对象
    @Getter
    private ProceedingJoinPoint joinPoint;

    // 生命周期实现
    @Getter
    private Lifecycle lifecycle;

    // 优先级小于0的插件
    @Getter
    private List<JanusPlugin> higherPluginList;

    // 优先级大于0的插件
    @Getter
    private List<JanusPlugin> lowerPluginList;

    // 比对功能实现
    @Getter
    private JanusCompare janusCompare;

    // 唯一标识
    @Getter
    private String methodId;

    // 业务数据键
    @Getter
    private String businessKey;

    // 比对类型
    @Getter
    private CompareType compareType;

    // 是否比对，允许通过插件在运行时判断是否要比对
    @Setter
    private Boolean isCompare;

    // 比对分支运行完后，比对2个分支的结果的过程是否异步执行。默认是异步执行
    private Boolean isAsyncCompare;

    // 主分支，只允许设置1次，不能随意修改该属性
    @Getter
    private String masterBranchName;

    // 加了 Janus 注解的分支
    @Getter
    private BranchInfoImpl primaryBranch;
    // 次要分支
    @Getter
    private BranchInfoImpl secondaryBranch;

    // 主分支
    @Getter
    @Setter
    private BranchInfoImpl masterBranch;
    // 用于比对的分支
    @Getter
    @Setter
    private BranchInfoImpl compareBranch;

    // 比对结果
    @Getter
    @Setter
    private CompareRes compareRes;

    // 自定义数据
    @Getter
    private Map<Class<?>, Object> pluginDataMap;

    // 比对时忽略的字段路径列表
    @Getter
    private Set<String> ignoreFieldPaths;

    public void setMasterBranchName(String masterBranchName) {
        // 只允许设置1次，不能随意修改该属性
        if (this.masterBranchName == null) {
            this.masterBranchName = masterBranchName;
        } else {
            throw new JanusException("masterBranchName 只能设置1次");
        }
    }

    public Boolean isCompare() {
        return this.isCompare;
    }

    public Boolean isNotCompare() {
        return !this.isCompare;
    }

    public Boolean isAsyncCompare() {
        return isAsyncCompare;
    }

    public Object getPluginData(Class<?> clazz) {
        return this.pluginDataMap.get(clazz);
    }

    public void putPluginData(Class<?> clazz, Object data) {
        this.pluginDataMap.put(clazz, data);
    }

    /**
     * 获取切点方法上面的指定注解
     *
     * @param annotationClass 注解类
     * @return 注解对象
     * @param <T> 注解类型泛型
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return JanusUtils.getAnnotation(joinPoint, annotationClass);
    }

    /**
     * 执行主分支
     */
    public void masterBranchExecute() {
        this.executeBranch(this.masterBranch);
    }

    /**
     * 执行比对分支
     */
    public void compareBranchExecute() {
        this.executeBranch(this.compareBranch);
    }

    /**
     * 执行某个分支
     *
     * @param branch 分支对象
     */
    private void executeBranch(BranchInfoImpl branch) {
        if (branch.getIsExecuted()) {
            // 该分支执行过，不再执行
            return;
        }
        if (branch == this.primaryBranch) {
            // 该分支是 primary 分支
            lifecycle.primaryExecute(this);
        } else {
            // 该分支是 secondary 分支
            lifecycle.secondaryExecute(this);
        }
    }
}
