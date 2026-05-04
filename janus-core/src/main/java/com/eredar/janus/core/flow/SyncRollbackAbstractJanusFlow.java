package com.eredar.janus.core.flow;

import com.eredar.janus.core.dto.JanusContextImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class SyncRollbackAbstractJanusFlow implements JanusFlow {

    @Autowired
    protected TransactionTemplate transactionTemplate;

    protected void executeBranchThenCompare(JanusContextImpl context) {
        // 开启总事务，让两个分支在同一事务上下文中执行，尽量保证数据一致性
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    // 先执行比对分支
                    context.compareBranchExecute();
                    // 再执行主分支
                    context.masterBranchExecute();
                } catch (Throwable e) {
                    // 必须转为 RuntimeException 才能触发事务回滚
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException(e);
                    }
                } finally {
                    // 同步 rollback-only 状态
                    if (status.isRollbackOnly()) {
                        status.setRollbackOnly();
                    }
                }
            }
        });

        // 比对分支已在事务中执行完毕，此处无需再次执行，直接进行比对
        if (context.doNotCompare()) {
            return;
        }

        context.compare();
    }
}
