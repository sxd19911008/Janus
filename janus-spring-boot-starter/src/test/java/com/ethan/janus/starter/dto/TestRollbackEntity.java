package com.ethan.janus.starter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试回滚表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRollbackEntity {

    private String tblKey;
    private Integer tblNum;
}