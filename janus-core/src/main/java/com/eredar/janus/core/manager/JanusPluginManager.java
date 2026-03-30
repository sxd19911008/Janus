package com.eredar.janus.core.manager;

import com.eredar.janus.core.annotation.Global;
import com.eredar.janus.core.exception.JanusException;
import com.eredar.janus.core.plugin.JanusPlugin;
import com.eredar.janus.core.utils.JanusAopUtils;
import com.eredar.janus.core.utils.JanusLogUtils;
import com.eredar.janus.core.utils.JanusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JanusPluginManager {

    private final Map<Class<? extends JanusPlugin>, JanusPlugin> globalPluginMap;
    private final Map<Class<? extends JanusPlugin>, JanusPlugin> methodPluginMap;

    public JanusPluginManager(List<JanusPlugin> janusPluginList) {
        this.globalPluginMap = new HashMap<>();
        this.methodPluginMap = new HashMap<>();
        if (JanusUtils.isNotEmpty(janusPluginList)) {
            for (JanusPlugin plugin : janusPluginList) {
                // 防止有动态代理类导致无法获取正确的class，先获取原始的bean对象
                JanusPlugin target = (JanusPlugin) JanusAopUtils.getProxyTarget(plugin);
                // bean 类型
                Class<? extends JanusPlugin> clazz = target.getClass();
                // 同一个插件有多个对象，会报错
                JanusPlugin pluginFromMap = this.methodPluginMap.get(clazz);
                if (pluginFromMap != null) {
                    throw new JanusException("Multiple plugins of type [" + clazz.getName() + "] found");
                }
                // 插件优先级是否合法
                if (plugin.getOrder() == 0) {
                    throw new JanusException("插件优先级不能为0");
                }

                // 保存插件
                Global annotation = AnnotationUtils.findAnnotation(clazz, Global.class);
                if (annotation == null) { // 非全局插件
                    this.methodPluginMap.put(clazz, plugin);
                } else { // 全局插件
                    this.globalPluginMap.put(clazz, plugin);
                }
            }
        }
    }

    /**
     * 批量获取方法级别的插件
     *
     * @param clazzArr 插件数组
     * @return 方法级别的插件列表
     */
    public List<JanusPlugin> getMethodPluginList(Class<? extends JanusPlugin>[] clazzArr) {
        if (clazzArr == null || clazzArr.length == 0) {
            return new ArrayList<>();
        }
        List<JanusPlugin> list = new ArrayList<>();
        for (Class<? extends JanusPlugin> aClass : clazzArr) {
            JanusPlugin janusPlugin = this.methodPluginMap.get(aClass);
            if (janusPlugin != null) {
                // 该插件为 方法级别的插件，正常放入list中
                list.add(janusPlugin);
            } else {
                janusPlugin = this.globalPluginMap.get(aClass);
                if (janusPlugin == null) {
                    // bean 未找到
                    throw new JanusException("No plugin of type [" + aClass.getName() + "] found");
                } else {
                    // 该插件为全局插件，不能放入list中，需要警告用户
                    log.error(
                            "[Janus] {} >> [{}]is a global plugin, do not configure in @Janus",
                            JanusLogUtils.FAIL_ICON,
                            aClass.getName()
                    );
                }
            }
        }
        return list;
    }

    /**
     * 获取所有的全局插件
     */
    public List<JanusPlugin> getAllGlobalPluginList() {
        return new ArrayList<>(this.globalPluginMap.values());
    }
}
