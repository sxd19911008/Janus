package com.eredar.janus.core.flow.impl;

import com.eredar.janus.core.constants.JanusCompareType;
import com.eredar.janus.core.dto.JanusContextImpl;
import com.eredar.janus.core.flow.SyncRollbackAbstractJanusFlow;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link JanusCompareType#SYNC_ROLLBACK_ONE_COMPARE} 流程实现
 * <p>在同一事务中同步执行两个分支，让两个分支尽量看到一致的数据库快照，两个分支执行完毕后进行比对。</p>
 * <p>通过 {@code RollbackLifecycle} 的子事务机制，回滚比对分支的事务，保证不污染数据库。</p>
 */
@Slf4j
public class SyncRollbackOneCompareJanusFlow extends SyncRollbackAbstractJanusFlow {

    @Override
    public String getCompareType() {
        return JanusCompareType.SYNC_ROLLBACK_ONE_COMPARE;
    }

    @Override
    public void execute(JanusContextImpl context) {
        // 回滚比对分支
        context.getCompareBranch().setIsRollback(true);
        // 执行2个分支后比对
        this.executeBranchThenCompare(context);
    }
}
