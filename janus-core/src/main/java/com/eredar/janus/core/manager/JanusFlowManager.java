package com.eredar.janus.core.manager;

import com.eredar.janus.core.config.JanusConfigProperties;
import com.eredar.janus.core.exception.JanusException;
import com.eredar.janus.core.flow.JanusFlow;
import com.eredar.janus.core.utils.JanusJsonUtils;
import com.eredar.janus.core.utils.JanusUtils;

import java.util.*;

public class JanusFlowManager {

    private final Map<String, JanusFlow> map;

    public JanusFlowManager(List<JanusFlow> list, JanusConfigProperties janusConfigProperties) {
        this.map = new HashMap<>();
        Set<String> duplicateSet = new HashSet<>();
        if (JanusUtils.isNotEmpty(list)) {
            for (JanusFlow flow : list) {
                JanusFlow existingFlow = map.get(flow.getCompareType());
                if (existingFlow != null) {
                    duplicateSet.add(flow.getCompareType());
                }
                map.put(flow.getCompareType(), flow);
            }
            if (JanusUtils.isNotEmpty(duplicateSet)) {
                throw new JanusException("这些CompareType重复了：" + JanusJsonUtils.writeValueAsString(duplicateSet));
            }
        }
        // 校验配置项是否正确
        if (this.doNotContainsCompareType(janusConfigProperties.getDefaultCompareType())) {
            throw new JanusException(String.format("配置项[janus.default-compare-type]的值[%s]错误", janusConfigProperties.getDefaultCompareType()));
        }
    }

    /**
     * 校验传入的 compareType 是否存在
     *
     * @param compareType 比对类型
     * @return true-存在；false-不存在
     */
    public boolean containsCompareType(String compareType) {
        return map.containsKey(compareType);
    }

    /**
     * 校验传入的 compareType 是否不存在
     *
     * @param compareType 比对类型
     * @return true-不存在；false-存在
     */
    public boolean doNotContainsCompareType(String compareType) {
        return !containsCompareType(compareType);
    }

    /**
     * 获取比对流程编排的实现类
     *
     * @param flowName 流程名
     * @return 具体实现对象
     */
    public JanusFlow getFlow(String flowName) {
        JanusFlow flow = this.map.get(flowName);
        if (flow == null) {
            // bean 未找到
            throw new JanusException(String.format("No JanusFlow of type [%s] found", flowName));
        }
        return flow;
    }
}
