package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.constants.JanusConstants;
import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import org.springframework.stereotype.Component;

@Component
public class SwitchJanusPlugin implements JanusPlugin {

    @Override
    public void switchBranch(JanusContext context) {
        context.setMasterBranchName(JanusConstants.PRIMARY);
    }
}
