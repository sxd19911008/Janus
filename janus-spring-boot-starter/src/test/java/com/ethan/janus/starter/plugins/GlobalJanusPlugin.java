package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.annotation.Global;
import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.starter.JanusTests;
import org.springframework.stereotype.Component;

@Global
@Component
public class GlobalJanusPlugin implements JanusPlugin {

    @Override
    public void switchBranch(JanusContext context) {
        JanusTests.globalString = "Global";
    }
}
