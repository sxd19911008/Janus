package com.ethan.janus.core.dto;

import com.ethan.janus.core.constants.CompareType;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface JanusContext {

    String getMethodId();
    String getBusinessKey();
    CompareType getCompareType();
    Boolean isCompare();
    Boolean isNotCompare();
    void setIsCompare(Boolean isCompare);
    Boolean isAsyncCompare();
    String getMasterBranchName();
    BranchInfo getPrimaryBranch();
    BranchInfo getSecondaryBranch();
    BranchInfo getMasterBranch();
    BranchInfo getCompareBranch();
    CompareRes getCompareRes();

    void setMasterBranchName(String masterBranchName);

    Set<String> getIgnoreFieldPaths();

    <T extends Annotation> T getAnnotation(Class<T> annotationClass);
}
