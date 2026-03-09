package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.constants.JanusConstants;
import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.starter.dto.TestRequest;
import org.springframework.stereotype.Component;

@Component
public class AsyncSwitchJanusPlugin implements JanusPlugin {

    @Override
    public void switchBranch(JanusContext context) {
        Object[] args = context.getArgs();
        TestRequest request = (TestRequest) args[0];
        if ("primary".equals(request.getKey())) {
            context.setMasterBranchName(JanusConstants.PRIMARY);
        } else {
            context.setMasterBranchName(JanusConstants.SECONDARY);
        }
    }
}
