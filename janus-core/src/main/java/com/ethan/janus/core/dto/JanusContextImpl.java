package com.ethan.janus.core.dto;

import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.core.exception.JanusException;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.core.lifecycle.Lifecycle;
import lombok.*;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.List;
import java.util.Map;

/**
 * Janus 对外暴露的上下文
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class JanusContextImpl implements JanusContext {

    // 切点对象
    private ProceedingJoinPoint joinPoint;

    // 生命周期实现
    private Lifecycle lifecycle;

    // 优先级小于0的插件
    private List<JanusPlugin> higherPluginList;

    // 优先级大于0的插件
    private List<JanusPlugin> lowerPluginList;

    // 唯一标识
    private String methodId;

    // 业务数据键
    private String businessKey;

    // 比对类型
    @Setter
    private CompareType compareType;

    private Boolean isAsyncCompare;

    // 主分支，只允许设置1次，不能随意修改该属性
    private String masterBranchName;

    // 加了 Janus 注解的分支
    private BranchInfoImpl primaryBranch;
    // 次要分支
    private BranchInfoImpl secondaryBranch;

    // 主分支
    @Setter
    private BranchInfoImpl masterBranch;
    // 用于比对的分支
    @Setter
    private BranchInfoImpl compareBranch;

    // 比对结果
    @Setter
    private Map<String, String> compareResMap;

    // 自定义数据
    private Map<Class<?>, Object> pluginDataMap;

    public void setMasterBranchName(String masterBranchName) {
        // 只允许设置1次，不能随意修改该属性
        if (this.masterBranchName == null) {
            this.masterBranchName = masterBranchName;
        } else {
            throw new JanusException("masterBranchName 只能设置1次");
        }
    }

    public Object getPluginData(Class<?> clazz) {
        return this.pluginDataMap.get(clazz);
    }

    public void putPluginData(Class<?> clazz, Object data) {
        this.pluginDataMap.put(clazz, data);
    }
}
