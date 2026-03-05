package com.ethan.janus.core.rollback;

import com.ethan.janus.core.dto.JanusContext;

public interface JanusRollback {

    void branchRollback(JanusContext context, String lifecycle, Runnable runnable);
}
