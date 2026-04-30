package com.eredar.janus.core.flow.impl;

import com.eredar.janus.core.constants.JanusCompareType;
import com.eredar.janus.core.dto.JanusContextImpl;
import com.eredar.janus.core.flow.JanusFlow;

/**
 * {@link JanusCompareType#DO_NOT_COMPARE} 流程实现
 * <p>只执行主分支，不执行比对分支，不进行任何比对。</p>
 */
public class DoNotCompareJanusFlow implements JanusFlow {

    @Override
    public String getCompareType() {
        return JanusCompareType.DO_NOT_COMPARE;
    }

    @Override
    public void execute(JanusContextImpl context) {
        // 仅执行主分支，直接返回，无需任何比对逻辑
        context.masterBranchExecute();
    }
}
