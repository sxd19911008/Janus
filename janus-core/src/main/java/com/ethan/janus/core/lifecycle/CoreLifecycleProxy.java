package com.ethan.janus.core.lifecycle;

import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.core.utils.JanusUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * 核心生命周期静态代理类。
 * <P>1. 每次进入aop都要new一个新的代理类，以此来更新pluginList
 * <P>2. 作为代理类，增强的功能为：在正确的生命周期节点执行配置的插件。
 */
public class CoreLifecycleProxy implements JanusLifecycle{

    private final JanusLifecycle target;
    private final List<JanusPlugin> pluginList;

    public CoreLifecycleProxy(JanusLifecycle target, List<JanusPlugin> pluginList) {
        this.target = target;
        this.pluginList = pluginList;
    }

    @Override
    public void switchBranch(JanusContext<?> context) {
        this.executePluginList(plugin -> plugin.switchBranch(context));
        target.switchBranch(context);
    }

    @Override
    public void primaryExecute(JanusContext<?> context) {
        this.executePluginList(plugin -> plugin.beforePrimaryExecute(context));
        target.primaryExecute(context);
        this.executePluginList(plugin -> plugin.afterPrimaryExecute(context));
    }

    @Override
    public void secondaryExecute(JanusContext<?> context) {
        this.executePluginList(plugin -> plugin.beforeSecondaryExecute(context));
        target.secondaryExecute(context);
        this.executePluginList(plugin -> plugin.afterSecondaryExecute(context));
    }

    @Override
    public void compare(JanusContext<?> context) {
        this.executePluginList(plugin -> plugin.beforeCompare(context));
        target.compare(context);
        this.executePluginList(plugin -> plugin.afterCompare(context));
    }

    /**
     * 批量执行插件列表
     *
     * @param consumer 被期望被执行的方法
     */
    private void executePluginList(Consumer<JanusPlugin> consumer) {
        if (JanusUtils.isNotEmpty(pluginList)) {
            pluginList.forEach(plugin -> consumer.accept(plugin));
        }
    }
}
