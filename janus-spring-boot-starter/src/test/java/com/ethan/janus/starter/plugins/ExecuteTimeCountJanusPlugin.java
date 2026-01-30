package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.lifecycle.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.starter.JanusTests;
import com.ethan.janus.starter.dto.JanusData;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;

@Component
public class ExecuteTimeCountJanusPlugin implements JanusPlugin<JanusData> {

    @Override
    public void switchBranch(JanusContext<JanusData> context) {
        context.setCustomData(new JanusData());
    }

    @Override
    public void beforePrimaryExecute(JanusContext<JanusData> context) {
        StopWatch sw = new StopWatch();
        context.getCustomData().setPrimaryStopWatch(sw);
        sw.start("Janus-primary");
        context.getCustomData().setPrimaryStartDate(LocalDateTime.now());
    }

    @Override
    public void afterPrimaryExecute(JanusContext<JanusData> context) {
        context.getCustomData().getPrimaryStopWatch().stop();
        context.getCustomData().setPrimaryEndDate(LocalDateTime.now());
    }

    @Override
    public void beforeSecondaryExecute(JanusContext<JanusData> context) {
        StopWatch sw = new StopWatch();
        context.getCustomData().setSecondaryStopWatch(sw);
        sw.start("Janus-secondary");
        context.getCustomData().setSecondaryStartDate(LocalDateTime.now());
    }

    @Override
    public void afterSecondaryExecute(JanusContext<JanusData> context) {
        context.getCustomData().getSecondaryStopWatch().stop();
        context.getCustomData().setSecondaryEndDate(LocalDateTime.now());
    }

    @Override
    public void beforeCompare(JanusContext<JanusData> context) {
        JanusData customData = context.getCustomData();
        // primary 分支耗时
        JanusTests.primaryTime = customData.getPrimaryStopWatch().getTotalTimeNanos();
        // secondary 分支耗时
        JanusTests.secondaryTime = customData.getSecondaryStopWatch().getTotalTimeNanos();
    }

    @Override
    public void afterCompare(JanusContext<JanusData> context) {
        JanusTests.compareResMap = context.getCompareResMap();
    }
}
