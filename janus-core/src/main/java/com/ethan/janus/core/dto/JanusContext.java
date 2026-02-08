package com.ethan.janus.core.dto;

import com.ethan.janus.core.constants.CompareType;

import java.util.Map;

public interface JanusContext {

    CompareType getCompareType();
    Boolean getIsAsyncCompare();
    String getMasterBranchName();
    BranchInfo getPrimaryBranch();
    BranchInfo getSecondaryBranch();
    BranchInfo getMasterBranch();
    BranchInfo getCompareBranch();
    Map<String, String> getCompareResMap();

    void setMasterBranchName(String masterBranchName);
}
