package com.eredar.janus.core.constants;

public class JanusCompareType {

    public static final String NONE = "NONE"; // 未设置
    public static final String DO_NOT_COMPARE = "DO_NOT_COMPARE"; // 只执行主分支，不比对
    public static final String SYNC_COMPARE = "SYNC_COMPARE"; // 同步执行2个分支，然后比对
    public static final String ASYNC_COMPARE = "ASYNC_COMPARE"; // 异步执行比对分支，然后比对
    public static final String SYNC_ROLLBACK_ONE_COMPARE = "SYNC_ROLLBACK_ONE_COMPARE"; // 同步执行2个分支，回滚比对分支的事务，然后比对
    public static final String SYNC_ROLLBACK_ALL_COMPARE = "SYNC_ROLLBACK_ALL_COMPARE"; // 同步执行2个分支，回滚2个分支的事务，然后比对

    /**
     * 是否需要比对
     *
     * @param compareType 比对类型
     * @return true-需要比对；false-不需要
     */
    public static boolean needCompare(String compareType) {
        return !JanusCompareType.DO_NOT_COMPARE.equals(compareType);
    }

    /**
     * 是否存在回滚操作
     *
     * @param compareType 比对类型
     * @return true-存在回滚操作 分支；false-不存在
     */
    public static boolean hasRollback(String compareType) {
        // 这写比对类型都涉及回滚
        return JanusCompareType.SYNC_ROLLBACK_ONE_COMPARE.equals(compareType) // 回滚比对分支
                || JanusCompareType.SYNC_ROLLBACK_ALL_COMPARE.equals(compareType); // 2个分支都回滚
    }
}
