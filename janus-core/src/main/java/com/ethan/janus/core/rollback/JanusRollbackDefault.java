package com.ethan.janus.core.rollback;

import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.exception.JanusException;
import com.ethan.janus.core.utils.JanusLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Slf4j
public class JanusRollbackDefault implements JanusRollback {

    @Autowired(required = false)
    private JanusRollbackClearCache janusRollbackClearCache;

    /**
     * 开启 子事务 并回滚
     * @param runnable 要回滚的分支代码
     */
    @Transactional(propagation = Propagation.NESTED) // 子事务，savepoint
    public void branchRollback(JanusContext context, String lifecycle, Runnable runnable) {
        log.debug(
                "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:{}] >> savepoint",
                JanusLogUtils.SUCCESS_ICON,
                context.getMethodId(),
                context.getBusinessKey(),
                lifecycle
        );
        try {
            runnable.run();
        } finally {
            // 通过 setRollbackOnly 回滚 savepoint
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.debug(
                    "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:{}] >> rollback",
                    JanusLogUtils.SUCCESS_ICON,
                    context.getMethodId(),
                    context.getBusinessKey(),
                    lifecycle
            );

            /*
             * 【注意】如果使用了类似 MyBatis 的框架，则执行完比对分支，要清理一级缓存，防止因为缓存导致查询错误。
             * 因为`connection.rollback(savepoint);`不会清理一级缓存。导致事务结束时，无法清理一级缓存。
             * 必须在事务结束之前，清理一级缓存！否则缓存清理不掉。一级缓存绑定事务。
             * 如果是报错导致事务回滚，则会自动清理一级缓存，没有此问题。
             * 以 MyBatis 的清理一级缓存做示例：  sqlSessionTemplate.clearCache();
             */
            if (janusRollbackClearCache == null) {
                //noinspection ThrowFromFinallyBlock
                throw new JanusException("请提供 JanusRollbackClearCache 实现");
            }
            janusRollbackClearCache.clearCache();
        }
    }
}
