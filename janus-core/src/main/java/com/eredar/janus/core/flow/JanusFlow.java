package com.eredar.janus.core.flow;

import com.eredar.janus.core.constants.JanusCompareType;
import com.eredar.janus.core.dto.JanusContextImpl;

/**
 * 根据 {@link JanusCompareType} 编排分流比对流程
 */
public interface JanusFlow {

    String getCompareType();

    void execute(JanusContextImpl context);
}
