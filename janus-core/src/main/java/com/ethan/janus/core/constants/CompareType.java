package com.ethan.janus.core.constants;

import lombok.Getter;

@Getter
public enum CompareType {
    NONE("未设置"),
    DO_NOT_COMPARE("只执行主分支，不比对"),
    SYNC_COMPARE("同步执行2个分支，然后比对"),
    ASYNC_COMPARE("异步执行比对分支，然后比对"),
    SYNC_ROLLBACK_ONE_COMPARE("同步执行2个分支，回滚比对分支的事务，然后比对"),
    SYNC_ROLLBACK_ALL_COMPARE("同步执行2个分支，回滚2个分支的事务，然后比对"),
    ;

    private final String description;

    CompareType(String description) {
        this.description = description;
    }

    /**
     * 是否异步执行比对分支
     * @param compareType 比对类型
     * @return true-异步执行比对分支；false-同步执行比对分支
     */
    public static boolean isAsyncCompareBranch(CompareType compareType) {
        return CompareType.ASYNC_COMPARE.equals(compareType);
    }

    /**
     * 是否回滚 master 分支
     *
     * @param compareType 比对类型
     * @return true-回滚 master 分支；false-不回滚 master 分支
     */
    public static boolean isMasterBranchRollback(CompareType compareType) {
        // 2个分支都回滚的场景，需要回滚 master 分支
        return CompareType.SYNC_ROLLBACK_ALL_COMPARE.equals(compareType);
    }

    /**
     * 是否回滚 compare 分支
     *
     * @param compareType 比对类型
     * @return true-回滚 master 分支；false-不回滚 master 分支
     */
    public static boolean isCompareBranchRollback(CompareType compareType) {
        // 回滚比对分支、2个分支都回滚 这2个场景，都需要回滚 比对 分支
        return CompareType.SYNC_ROLLBACK_ONE_COMPARE.equals(compareType) // 回滚比对分支
                || CompareType.SYNC_ROLLBACK_ALL_COMPARE.equals(compareType); // 2个分支都回滚
    }

    /**
     * 是否存在回滚操作
     *
     * @param compareType 比对类型
     * @return true-存在回滚操作 分支；false-不存在
     */
    public static boolean hasRollback(CompareType compareType) {
        // 这写比对类型都涉及回滚
        return CompareType.SYNC_ROLLBACK_ONE_COMPARE.equals(compareType) // 回滚比对分支
                || CompareType.SYNC_ROLLBACK_ALL_COMPARE.equals(compareType); // 2个分支都回滚
    }
}
