package com.eredar.janus.core.manager;

import com.eredar.janus.core.compare.JanusCompare;
import com.eredar.janus.core.exception.JanusException;
import com.eredar.janus.core.utils.JanusAopUtils;
import com.eredar.janus.core.utils.JanusUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一管理所有 自定义比对实现
 */
public class JanusCompareManager {

    private final Map<Class<? extends JanusCompare>, JanusCompare> map;

    public JanusCompareManager(List<JanusCompare> compareList) {
        this.map = new HashMap<>();
        if (JanusUtils.isNotEmpty(compareList)) {
            for (JanusCompare janusCompare : compareList) {
                // 防止有动态代理类导致无法获取正确的class，先获取原始的bean对象
                JanusCompare target = (JanusCompare) JanusAopUtils.getProxyTarget(janusCompare);
                // bean 类型
                Class<? extends JanusCompare> clazz = target.getClass();
                map.put(clazz, janusCompare);
            }
        }
    }

    /**
     * 获取比对实现
     *
     * @param clazz 具体实现的类对象
     * @return 具体实现对象
     */
    public JanusCompare getJanusCompare(Class<? extends JanusCompare> clazz) {
        JanusCompare janusCompare = this.map.get(clazz);
        if (janusCompare == null) {
            // bean 未找到
            throw new JanusException("No JanusCompare of type [" + clazz.getName() + "] found");
        }
        return janusCompare;
    }
}
