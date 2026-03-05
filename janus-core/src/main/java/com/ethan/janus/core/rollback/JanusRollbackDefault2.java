//package com.ethan.janus.core.rollback;
//
//import com.ethan.janus.core.dto.JanusContext;
//import com.ethan.janus.core.exception.JanusException;
//import com.ethan.janus.core.utils.JanusLogUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jdbc.datasource.ConnectionHolder;
//import org.springframework.jdbc.datasource.DataSourceUtils;
//import org.springframework.transaction.support.TransactionSynchronizationManager;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.sql.Savepoint;
//import java.util.UUID;
//
///**
// * 相比 JanusRollbackDefault，该实现允许 setSavepoint(String name) 时自定义 name
// */
//@Slf4j
//public class JanusRollbackDefault2 implements JanusRollback {
//
//    @Autowired(required = false)
//    private DataSource dataSource;
//    @Autowired(required = false)
//    private JanusRollbackClearCache janusRollbackClearCache;
//
//    public void branchRollback(JanusContext context, String lifecycle, Runnable runnable) {
//        if (dataSource == null) {
//            runnable.run();
//            return;
//        }
//        try {
//            Connection connection = DataSourceUtils.getConnection(dataSource);
//            String uuid = UUID.randomUUID().toString();
//            // 事务保存点
//            Savepoint savepoint;
//            try {
//                savepoint = connection.setSavepoint(uuid);
//                log.debug(
//                        "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:{}] >> savepoint",
//                        JanusLogUtils.SUCCESS_ICON,
//                        context.getMethodId(),
//                        context.getBusinessKey(),
//                        lifecycle
//                );
//            } catch (SQLException e) {
//                throw new JanusException("savepoint 异常", e);
//            }
//            try {
//                // 执行分支方法
//                runnable.run();
//            } finally {
//                try {
//                    // 回滚到事务保存点，而不是回滚整个事务。
//                    connection.rollback(savepoint);
//                    log.debug(
//                            "[Janus] {} [methodId:{}] [businessKey:{}] [lifecycle:{}] >> rollback",
//                            JanusLogUtils.SUCCESS_ICON,
//                            context.getMethodId(),
//                            context.getBusinessKey(),
//                            lifecycle
//                    );
//                } catch (SQLException e) {
//                    //noinspection ThrowFromFinallyBlock
//                    throw new JanusException("savepoint回滚报错", e);
//                }
//
//                /*
//                 * 【注意】如果使用了类似 MyBatis 的框架，则执行完比对分支，要清理一级缓存，防止因为缓存导致查询错误。
//                 * 因为`connection.rollback(savepoint);`不会清理一级缓存。导致事务结束时，无法清理一级缓存。
//                 * 必须在事务结束之前，清理一级缓存！否则缓存清理不掉。一级缓存绑定事务。
//                 * 如果是报错导致事务回滚，则会自动清理一级缓存，没有此问题。
//                 * 以 MyBatis 的清理一级缓存做示例：  sqlSessionTemplate.clearCache();
//                 */
//                if (janusRollbackClearCache == null) {
//                    //noinspection ThrowFromFinallyBlock
//                    throw new JanusException("请提供 JanusRollbackClearCache 实现");
//                }
//                janusRollbackClearCache.clearCache();
//            }
//        } finally {
//            /*
//             * 【重要】必须自己开的事务结束后再 resetRollbackOnly
//             * 【重要】必须单独写一个 finally 来 resetRollbackOnly
//             */
//            ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
//            if (holder != null && holder.isRollbackOnly()) {
//                // 【重要】比对分支报错，主分支不可回滚，所以要重置 rollbackOnly 为 false
//                holder.resetRollbackOnly();
//            }
//        }
//    }
//}
