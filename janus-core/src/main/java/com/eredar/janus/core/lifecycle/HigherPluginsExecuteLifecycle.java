package com.eredar.janus.core.lifecycle;

import com.eredar.janus.core.dto.JanusContextImpl;
import com.eredar.janus.core.plugin.JanusPlugin;

import java.util.List;

/**
 * 核心生命周期静态装饰类。
 * <P>1. 作为装饰类，增强的功能为：在正确的生命周期节点执行配置的插件。
 * <P>2. 仅执行优先级小于0的插件。
 */
public class HigherPluginsExecuteLifecycle extends LifecycleDecorator {

    @Override
    public void switchBranch(JanusContextImpl context) {
        for (JanusPlugin plugin : context.getHigherPluginList()) {
            plugin.switchBranch(context);
        }
        decoratedLifecycle.switchBranch(context);
    }

    @Override
    public void primaryExecute(JanusContextImpl context) {
        List<JanusPlugin> pluginList = context.getHigherPluginList();
        for (JanusPlugin plugin : pluginList) {
            plugin.beforePrimaryExecute(context);
        }
        decoratedLifecycle.primaryExecute(context);
        // 倒序执行所有插件的 after 方法
        for (int i = pluginList.size() - 1; i >= 0; i--) {
            pluginList.get(i).afterPrimaryExecute(context);
        }
    }

    @Override
    public void secondaryExecute(JanusContextImpl context) {
        List<JanusPlugin> pluginList = context.getHigherPluginList();
        for (JanusPlugin plugin : pluginList) {
            plugin.beforeSecondaryExecute(context);
        }
        decoratedLifecycle.secondaryExecute(context);
        // 倒序执行所有插件的 after 方法
        for (int i = pluginList.size() - 1; i >= 0; i--) {
            pluginList.get(i).afterSecondaryExecute(context);
        }
    }

    @Override
    public void compare(JanusContextImpl context) {
        List<JanusPlugin> pluginList = context.getHigherPluginList();
        for (JanusPlugin plugin : pluginList) {
            plugin.beforeCompare(context);
        }
        decoratedLifecycle.compare(context);
        // 倒序执行所有插件的 after 方法
        for (int i = pluginList.size() - 1; i >= 0; i--) {
            pluginList.get(i).afterCompare(context);
        }
    }
}
