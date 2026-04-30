package com.eredar.janus.core.dto;

import com.eredar.janus.core.plugin.AbstractDataJanusPlugin;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface JanusContext {

    /**
     * 获取方法的唯一标识
     */
    String getMethodId();

    /**
     * 获取业务主键
     */
    String getBusinessKey();

    /**
     * 获取比对类型
     */
    String getCompareType();

    /**
     * 获取方法入参
     */
    Object[] getArgs();

    /**
     * 是否比对
     *
     * @return true-比对；false-不比对
     */
    Boolean needCompare();

    /**
     * 是否不比对
     *
     * @return true-不比对；false-比对
     */
    Boolean doNotCompare();

    /**
     * 设置是否比对
     */
    void setNeedCompare(Boolean needCompare);

    /**
     * 是否异步比对
     *
     * @return true-使用 janusCompareThreadPool 线程池异步比对；false-同步比对，不使用线程池
     */
    Boolean isAsyncCompare();

    /**
     * 获取主分支名称
     */
    String getMasterBranchName();

    /**
     * 获取primary分支
     */
    BranchInfo getPrimaryBranch();

    /**
     * 获取secondary分支
     */
    BranchInfo getSecondaryBranch();

    /**
     * 获取主分支
     */
    BranchInfo getMasterBranch();

    /**
     * 获取比对分支
     */
    BranchInfo getCompareBranch();

    /**
     * 设置primary分支的查询结果
     */
    void setPrimaryQueryRes(Object queryRes);

    /**
     * 设置secondary分支的查询结果
     */
    void setSecondaryQueryRes(Object queryRes);

    /**
     * 获取比对结果
     */
    CompareRes getCompareRes();

    /**
     * 设置主分支名称
     */
    void setMasterBranchName(String masterBranchName);

    /**
     * 获取比对时需要忽略的字段路径集合
     */
    Set<String> getIgnoreFieldPaths();

    /**
     * 获取自定义注解
     *
     * @param annotationClass 注解类型
     * @param <T>             注解的泛型类型
     * @return 注解对象
     */
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * 获取primary分支的执行耗时（纳秒）
     */
    Long getPrimaryTime();

    /**
     * 获取secondary分支的执行耗时（纳秒）
     */
    Long getSecondaryTime();

    /**
     * 获取其他插件的数据
     *
     * @param pluginClass 其他插件的类型
     * @param <OTH>       插件的数据泛型
     * @return 其他插件的数据对象
     */
    <OTH> OTH getOtherPluginData(Class<? extends AbstractDataJanusPlugin<OTH>> pluginClass);
}
