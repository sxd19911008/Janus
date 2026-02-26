package com.ethan.janus.core.compare;

import com.ethan.janus.core.dto.CompareRes;
import com.ethan.janus.core.dto.JanusContext;

/**
 * Janus 比对功能抽象
 */
public interface JanusCompare {

    CompareRes compare(JanusContext context);
}
