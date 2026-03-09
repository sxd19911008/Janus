package com.ethan.janus.starter.service;

import com.ethan.janus.core.annotation.Secondary;
import com.ethan.janus.starter.dao.TestRollbackMapper;
import com.ethan.janus.starter.dto.TestIgnoreDTO;
import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;
import com.ethan.janus.starter.dto.TestRollbackEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Secondary
@Service
public class SecondaryService implements TestInterface {

    @Autowired
    private TestRollbackMapper testRollbackMapper;

    @Override
    public TestResponse testAsyncCompare1(TestRequest request) {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return TestResponse.builder()
                .number(0)
                .build();
    }

    @Override
    public TestResponse testAsyncCompare2(TestRequest request) {
        return TestResponse.builder()
                .number(0)
                .build();
    }

    @Override
    public TestResponse testSyncCompare(TestRequest request) {
        if ("1".equals(request.getKey())) {
            return TestResponse.builder()
                    .number(1)
                    .build();
        } else if ("2".equals(request.getKey())) {
            return TestResponse.builder()
                    .number(3)
                    .build();
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return TestResponse.builder()
                .number(0)
                .build();
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public TestResponse testRollbackOne(TestRequest request) {
        String key = request.getKey();
        if ("a".equals(key)) {
            Integer existNum = testRollbackMapper.selectNumByKey("exist");
            testRollbackMapper.updateByKey("exist", existNum + 1);
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(1)
                    .build());
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(2)
                    .build());
            testRollbackMapper.deleteByKey("delete");
        } else if ("compareBranch_err".equals(key)) {
            Integer existNum = testRollbackMapper.selectNumByKey("exist");
            testRollbackMapper.updateByKey("exist", existNum + 1);
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(1)
                    .build());
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(2)
                    .build());
            testRollbackMapper.deleteByKey("delete");
        } else if ("masterBranch_err".equals(key)) {
            Integer existNum = testRollbackMapper.selectNumByKey("exist");
            testRollbackMapper.updateByKey("exist", existNum + 1);
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(1)
                    .build());
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(2)
                    .build());
            testRollbackMapper.deleteByKey("delete");
            @SuppressWarnings({"NumericOverflow", "divzero", "unused"}) int a = 2 / 0;
        }
        return TestResponse.builder()
                .number(0)
                .build();
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public TestResponse testRollbackAll(TestRequest request) {
        String key = request.getKey();
        if ("a".equals(key)) {
            Integer existNum = testRollbackMapper.selectNumByKey("exist");
            testRollbackMapper.updateByKey("exist", existNum + 1);
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(1)
                    .build());
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(2)
                    .build());
            testRollbackMapper.deleteByKey("delete");
        } else if ("compareBranch_err".equals(key)) {
            Integer existNum = testRollbackMapper.selectNumByKey("exist");
            testRollbackMapper.updateByKey("exist", existNum + 1);
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(1)
                    .build());
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(2)
                    .build());
            testRollbackMapper.deleteByKey("delete");
        } else if ("masterBranch_err".equals(key)) {
            Integer existNum = testRollbackMapper.selectNumByKey("exist");
            testRollbackMapper.updateByKey("exist", existNum + 1);
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(1)
                    .build());
            testRollbackMapper.insert(TestRollbackEntity.builder()
                    .tblKey(key)
                    .tblNum(2)
                    .build());
            testRollbackMapper.deleteByKey("delete");
            @SuppressWarnings({"NumericOverflow", "divzero", "unused"}) int a = 2 / 0;
        }
        return TestResponse.builder()
                .number(0)
                .build();
    }

    @Override
    public TestResponse testIgnore() {
        return TestResponse.builder()
                .number(1)
                .ignoreStr1("456")
                .ignoreList(new ArrayList<>(Arrays.asList(
                        TestIgnoreDTO.builder().str1("1").str2("2.1").build(),
                        TestIgnoreDTO.builder().str1("1").str2("2.1").build()
                )))
                .build();
    }
}
