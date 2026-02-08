package com.ethan.janus.core.dto;

public interface BranchInfo {

    Boolean getIsExecuted();

    String getBranchType();

    Object getRes();

    Throwable getException();

    Boolean getIsRollback();

    Boolean getIsAsync();
}
