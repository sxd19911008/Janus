package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.starter.JanusThrottlingTests;
import org.springframework.stereotype.Component;

@Component
public class AsyncResJanusPlugin implements JanusPlugin {

    @Override
    public void afterCompare(JanusContext context) {
        JanusThrottlingTests.pluginRes.businessKey = context.getBusinessKey();
        JanusThrottlingTests.pluginRes.masterBranchName = context.getMasterBranchName();
        JanusThrottlingTests.pluginRes.compareRes = context.getCompareRes();
        JanusThrottlingTests.pluginRes.methodId = context.getMethodId();
    }
}
