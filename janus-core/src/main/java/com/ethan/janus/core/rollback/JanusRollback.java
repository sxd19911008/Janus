package com.ethan.janus.core.rollback;

public interface JanusRollback {

    void branchRollback(Runnable runnable);
}
