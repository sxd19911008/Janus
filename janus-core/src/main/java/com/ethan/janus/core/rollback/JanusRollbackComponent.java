package com.ethan.janus.core.rollback;

import com.ethan.janus.core.exception.JanusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.UUID;

@ConditionalOnMissingBean(JanusRollback.class)
public class JanusRollbackComponent implements JanusRollback {

    @Autowired(required = false)
    private DataSource dataSource;

    @Transactional(rollbackFor = Throwable.class)
    public void branchRollback(Runnable runnable) {
        if (dataSource == null) {
            runnable.run();
            return;
        }
        Connection connection = DataSourceUtils.getConnection(dataSource);
        String uuid = UUID.randomUUID().toString();
        Savepoint savepoint;
        try {
            savepoint = connection.setSavepoint(uuid);
        } catch (SQLException e) {
            throw new JanusException("savepoint 异常", e);
        }
        try {
            runnable.run();
        } finally {
            // 【注意】比对分支报错，主分支不可回滚，所以要重置 rollbackOnly 为 false
            ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
            if (holder != null && holder.isRollbackOnly()) {
                holder.resetRollbackOnly();
            }
            try {
                connection.rollback(savepoint);
            } catch (SQLException e) {
                // TODO 日志框架
                e.printStackTrace();
            }
        }
        /*
         * TODO【注意】如果使用了类似MyBatis的框架，则执行完比对分支，要清除1级缓存，防止因为缓存导致查询错误。
         * 因为 connection.rollback(savepoint); 不会清除一级缓存，会导致事务结束时，无法清理一级缓存。
         * 必须在事务结束之前，清理一级缓存！否则缓存清理不掉。一级缓存绑定事务。
         * 如果是报错导致事务回滚，则会自动清理一级缓存，没有此问题。
         */
    }
}
