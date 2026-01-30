package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.constants.JanusConstants;
import com.ethan.janus.core.lifecycle.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.starter.dto.JanusData;
import org.springframework.stereotype.Component;

@Component
public class SwitchJanusPlugin implements JanusPlugin<JanusData> {

    @Override
    public void switchBranch(JanusContext<JanusData> context) {
        context.setMasterBranchName(JanusConstants.PRIMARY);
    }
}
