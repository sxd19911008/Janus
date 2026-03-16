package com.eredar.janus.starter.plugins;

import com.eredar.janus.core.dto.JanusContext;
import com.eredar.janus.core.plugin.AbstractDataJanusPlugin;
import com.eredar.janus.core.utils.JanusJsonUtils;
import com.eredar.janus.starter.JanusTests;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ExecuteTimeJanusPlugin extends AbstractDataJanusPlugin<ExecuteTimeJanusPlugin.ExecuteTimeJanusPluginData> {

    @Override
    public void beforeCompare(JanusContext context) {
        // primary 分支耗时
        JanusTests.pluginRes.primaryTime = context.getPrimaryTime();
        // secondary 分支耗时
        JanusTests.pluginRes.secondaryTime = context.getSecondaryTime();
    }

    @Override
    public void afterCompare(JanusContext context) {
        JanusTests.pluginRes.methodId = context.getMethodId();
        JanusTests.pluginRes.masterBranchName = context.getMasterBranchName();
        JanusTests.pluginRes.compareRes = context.getCompareRes();
        JanusTests.pluginRes.businessKey = context.getBusinessKey();
        TestAnnotationJanusPlugin.TestAnnotationJanusPluginData testAnnotationJanusPluginData = context.getOtherPluginData(TestAnnotationJanusPlugin.class);
        if (testAnnotationJanusPluginData != null) {
            JanusTests.pluginRes.testAnnotationKey = testAnnotationJanusPluginData.getValue();
        }

        log.debug(
                "primaryRes={}, secondaryRes={}, compareRes={}",
                JanusJsonUtils.writeValueAsString(context.getPrimaryBranch().getBranchRes()),
                JanusJsonUtils.writeValueAsString(context.getSecondaryBranch().getBranchRes()),
                JanusJsonUtils.writeValueAsString(context.getCompareRes())
        );
    }

    @Data
    public static class ExecuteTimeJanusPluginData {

        private LocalDateTime primaryStartDate;
        private LocalDateTime primaryEndDate;
        private LocalDateTime secondaryStartDate;
        private LocalDateTime secondaryEndDate;
        private StopWatch primaryStopWatch;
        private StopWatch secondaryStopWatch;
    }
}
