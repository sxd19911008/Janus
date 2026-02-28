package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.plugin.AbstractDataJanusPlugin;
import com.ethan.janus.core.utils.JanusJsonUtils;
import com.ethan.janus.starter.JanusTests;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ExecuteTimeJanusPlugin extends AbstractDataJanusPlugin<ExecuteTimeJanusPlugin.ExecuteTimeJanusPluginData> {

    @Override
    public void beforePrimaryExecute(JanusContext context) {
        ExecuteTimeJanusPluginData pluginData = this.getPluginData(context);
        StopWatch sw = new StopWatch();
        pluginData.setPrimaryStopWatch(sw);
        sw.start("Janus-primary");
        pluginData.setPrimaryStartDate(LocalDateTime.now());
    }

    @Override
    public void afterPrimaryExecute(JanusContext context) {
        ExecuteTimeJanusPluginData pluginData = this.getPluginData(context);
        pluginData.getPrimaryStopWatch().stop();
        pluginData.setPrimaryEndDate(LocalDateTime.now());
    }

    @Override
    public void beforeSecondaryExecute(JanusContext context) {
        ExecuteTimeJanusPluginData pluginData = this.getPluginData(context);
        StopWatch sw = new StopWatch();
        pluginData.setSecondaryStopWatch(sw);
        sw.start("Janus-secondary");
        pluginData.setSecondaryStartDate(LocalDateTime.now());
    }

    @Override
    public void afterSecondaryExecute(JanusContext context) {
        ExecuteTimeJanusPluginData pluginData = this.getPluginData(context);
        pluginData.getSecondaryStopWatch().stop();
        pluginData.setSecondaryEndDate(LocalDateTime.now());
    }

    @Override
    public void beforeCompare(JanusContext context) {
        ExecuteTimeJanusPluginData pluginData = this.getPluginData(context);
        // primary 分支耗时
        JanusTests.pluginRes.primaryTime = pluginData.getPrimaryStopWatch().getTotalTimeNanos();
        // secondary 分支耗时
        JanusTests.pluginRes.secondaryTime = pluginData.getSecondaryStopWatch().getTotalTimeNanos();
    }

    @Override
    public void afterCompare(JanusContext context) {
        JanusTests.pluginRes.methodId = context.getMethodId();
        JanusTests.pluginRes.compareRes = context.getCompareRes();
        JanusTests.pluginRes.businessKey = context.getBusinessKey();
        TestAnnotationJanusPlugin.TestAnnotationJanusPluginData testAnnotationJanusPluginData = this.getOtherPluginData(context, TestAnnotationJanusPlugin.class);
        if (testAnnotationJanusPluginData != null) {
            JanusTests.pluginRes.testAnnotationKey = testAnnotationJanusPluginData.getValue();
        }

        log.info(
                "primaryRes={}, secondaryRes={}, compareRes={}",
                JanusJsonUtils.writeValueAsString(context.getPrimaryBranch().getBranchRes()),
                JanusJsonUtils.writeValueAsString(context.getSecondaryBranch().getBranchRes()),
                JanusJsonUtils.writeValueAsString(context.getCompareRes())
        );
    }

    public static class ExecuteTimeJanusPluginData {

        private LocalDateTime primaryStartDate;
        private LocalDateTime primaryEndDate;
        private LocalDateTime secondaryStartDate;
        private LocalDateTime secondaryEndDate;
        private StopWatch primaryStopWatch;
        private StopWatch secondaryStopWatch;

        public LocalDateTime getPrimaryStartDate() {
            return primaryStartDate;
        }

        public void setPrimaryStartDate(LocalDateTime primaryStartDate) {
            this.primaryStartDate = primaryStartDate;
        }

        public LocalDateTime getPrimaryEndDate() {
            return primaryEndDate;
        }

        public void setPrimaryEndDate(LocalDateTime primaryEndDate) {
            this.primaryEndDate = primaryEndDate;
        }

        public LocalDateTime getSecondaryStartDate() {
            return secondaryStartDate;
        }

        public void setSecondaryStartDate(LocalDateTime secondaryStartDate) {
            this.secondaryStartDate = secondaryStartDate;
        }

        public LocalDateTime getSecondaryEndDate() {
            return secondaryEndDate;
        }

        public void setSecondaryEndDate(LocalDateTime secondaryEndDate) {
            this.secondaryEndDate = secondaryEndDate;
        }

        public StopWatch getPrimaryStopWatch() {
            return primaryStopWatch;
        }

        public void setPrimaryStopWatch(StopWatch primaryStopWatch) {
            this.primaryStopWatch = primaryStopWatch;
        }

        public StopWatch getSecondaryStopWatch() {
            return secondaryStopWatch;
        }

        public void setSecondaryStopWatch(StopWatch secondaryStopWatch) {
            this.secondaryStopWatch = secondaryStopWatch;
        }
    }
}
