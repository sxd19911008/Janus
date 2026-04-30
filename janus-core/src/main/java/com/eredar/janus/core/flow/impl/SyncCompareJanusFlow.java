package com.eredar.janus.core.flow.impl;

import com.eredar.janus.core.constants.JanusCompareType;
import com.eredar.janus.core.dto.JanusContextImpl;
import com.eredar.janus.core.flow.JanusFlow;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link JanusCompareType#SYNC_COMPARE} 流程实现
 * <p>同步执行主分支，再同步执行比对分支，最后进行比对。
 * 两个分支均在调用方线程中串行执行，主分支返回值对用户无感知延迟，
 * 但整体耗时为两个分支之和。</p>
 */
@Slf4j
public class SyncCompareJanusFlow implements JanusFlow {

    @Override
    public String getCompareType() {
        return JanusCompareType.SYNC_COMPARE;
    }

    @Override
    public void execute(JanusContextImpl context) {
        // 执行主分支
        context.masterBranchExecute();

        // 插件可能在主分支执行期间动态关闭比对，需再次检查
        if (context.doNotCompare()) {
            return;
        }

        // 同步执行比对分支
        context.compareBranchExecute();

        // 比对两个分支的结果
        context.compare();
    }
}
