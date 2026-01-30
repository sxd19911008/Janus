package com.ethan.janus.core.constants;

import lombok.Getter;

@Getter
public enum CompareType {
    NONE("未设置"),
    DO_NOT_COMPARE("不比对"),
    SYNC_COMPARE("同步比对"),
    ASYNC_COMPARE("异步比对"),
    ;

    private final String description;

    CompareType(String description) {
        this.description = description;
    }
}
