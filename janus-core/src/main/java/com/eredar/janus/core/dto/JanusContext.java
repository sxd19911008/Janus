package com.eredar.janus.core.dto;

import com.eredar.janus.core.constants.CompareType;
import com.eredar.janus.core.plugin.AbstractDataJanusPlugin;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface JanusContext {

    String getMethodId();

    String getBusinessKey();

    CompareType getCompareType();

    Object[] getArgs();

    Boolean isCompare();

    Boolean isNotCompare();

    void setIsCompare(Boolean isCompare);

    Boolean isAsyncCompare();

    String getMasterBranchName();

    BranchInfo getPrimaryBranch();

    BranchInfo getSecondaryBranch();

    BranchInfo getMasterBranch();

    BranchInfo getCompareBranch();

    void setPrimaryQueryRes(Object queryRes);

    void setSecondaryQueryRes(Object queryRes);

    CompareRes getCompareRes();

    void setMasterBranchName(String masterBranchName);

    Set<String> getIgnoreFieldPaths();

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    Long getPrimaryTime();

    Long getSecondaryTime();

    <OTH> OTH getOtherPluginData(Class<? extends AbstractDataJanusPlugin<OTH>> pluginClass);
}
