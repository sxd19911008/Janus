package com.ethan.janus.core.config;

import com.ethan.janus.core.exception.JanusException;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.core.utils.JanusAopUtils;
import com.ethan.janus.core.utils.JanusUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JanusPluginManager implements ApplicationRunner {

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<Class<? extends JanusPlugin>, JanusPlugin> pluginMap = new HashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        Map<String, JanusPlugin> beanMap = applicationContext.getBeansOfType(JanusPlugin.class);
        if (JanusUtils.isNotEmpty(beanMap)) {
            for (JanusPlugin plugin : beanMap.values()) {
                // 防止有动态代理类导致无法获取正确的class，先获取原始的bean对象
                JanusPlugin target = (JanusPlugin) JanusAopUtils.getProxyTarget(plugin);
                // 插件类型
                Class<? extends JanusPlugin> clazz = target.getClass();
                // 同一个插件有多个对象，会报错
                JanusPlugin pluginFromMap = this.pluginMap.get(clazz);
                if (pluginFromMap != null) {
                    throw new JanusException("Multiple plugins of type [" + clazz.getName() + "] found");
                }
                // 插件优先级是否合法
                if (plugin.getOrder() == 0) {
                    throw new JanusException("插件优先级不能为0");
                }
                // 保存插件
                this.pluginMap.put(clazz, plugin);
            }
        }
    }

    /**
     * 获取插件
     *
     * @param clazz 插件类
     * @return 插件单例对象
     */
    public JanusPlugin getJanusPlugin(Class<? extends JanusPlugin> clazz) {
        JanusPlugin janusPlugin = this.pluginMap.get(clazz);
        if (janusPlugin == null) {
            throw new JanusException("No plugin of type [" + clazz.getName() + "] found");
        }
        return janusPlugin;
    }

    /**
     * 批量获取插件
     * @param clazzArr 插件数组
     * @return 插件列表
     */
    public List<JanusPlugin> getJanusPluginList(Class<? extends JanusPlugin>[] clazzArr) {
        if (clazzArr == null || clazzArr.length == 0) {
            return new ArrayList<>();
        }
        List<JanusPlugin> list = new ArrayList<>();
        for (Class<? extends JanusPlugin> aClass : clazzArr) {
            JanusPlugin janusPlugin = this.getJanusPlugin(aClass);
            list.add(janusPlugin);
        }
        return list;
    }
}
