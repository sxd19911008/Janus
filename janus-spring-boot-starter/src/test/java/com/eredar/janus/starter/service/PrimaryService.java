package com.eredar.janus.starter.service;

import com.eredar.janus.core.annotation.Janus;
import com.eredar.janus.core.constants.CompareType;
import com.eredar.janus.starter.annotation.TestAnnotation;
import com.eredar.janus.starter.dao.TestRollbackMapper;
import com.eredar.janus.starter.dto.TestIgnoreDTO;
import com.eredar.janus.starter.dto.TestRequest;
import com.eredar.janus.starter.dto.TestResponse;
import com.eredar.janus.starter.dto.TestRollbackEntity;
import com.eredar.janus.starter.plugins.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;

@Primary
@Service
public class PrimaryService implements TestInterface {

    @Autowired
    private TestRollbackMapper testRollbackMapper;

    @Janus(
            methodId = "testAsyncCompare1",
            compareType = CompareType.ASYNC_COMPARE,
            businessKey = "#request.key",
            plugins = {AsyncSwitchJanusPlugin.class, CountCompareJanusPlugin.class}
    )
    public TestResponse testAsyncCompare1(TestRequest request) {
        return TestResponse.builder()
                .number(0)
                .build();
    }

    @Janus(
            methodId = "testAsyncCompare2",
            compareType = CompareType.ASYNC_COMPARE,
            businessKey = "#request.key",
            plugins = {AsyncSwitchJanusPlugin.class, AsyncResJanusPlugin.class}
    )
    @Override
    public TestResponse testAsyncCompare2(TestRequest request) {
        return TestResponse.builder()
                .number(0)
                .build();
    }

    @Janus(
            methodId = "testSyncCompare",
            compareType = CompareType.SYNC_COMPARE,
            isAsyncCompare = false,
            businessKey = "buildKey(#request.key, 'qqq')",
            plugins = {SwitchJanusPlugin.class, TestAnnotationJanusPlugin.class, ExecuteTimeJanusPlugin.class}
    )
    @TestAnnotation(value = "Archimonde")
    @Override
    public TestResponse testSyncCompare(TestRequest request) {
        if ("1".equals(request.getKey())) {
            return TestResponse.builder()
                    .number(1)
                    .build();
        } else if ("2".equals(request.getKey())) {
            return TestResponse.builder()
                    .number(2)
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

    @Janus(
            methodId = "testRollbackOne",
            compareType = CompareType.SYNC_ROLLBACK_ONE_COMPARE,
            isAsyncCompare = false,
            businessKey = "#request.key",
            plugins = {TestRollbackQueryDataJanusPlugin.class, ExecuteTimeJanusPlugin.class}
    )
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
            @SuppressWarnings({"NumericOverflow", "divzero", "unused"}) int a = 2 / 0;
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
        }
        return TestResponse.builder()
                .number(0)
                .build();
    }

    @Janus(
            methodId = "testRollbackAll",
            compareType = CompareType.SYNC_ROLLBACK_ALL_COMPARE,
            isAsyncCompare = false,
            businessKey = "#request.key",
            plugins = {TestRollbackQueryDataJanusPlugin.class, ExecuteTimeJanusPlugin.class}
    )
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
            @SuppressWarnings({"NumericOverflow", "divzero", "unused"}) int a = 2 / 0;
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
        }
        return TestResponse.builder()
                .number(0)
                .build();
    }

    @Janus(
            methodId = "testIgnore",
            compareType = CompareType.SYNC_COMPARE,
            isAsyncCompare = false,
            plugins = ExecuteTimeJanusPlugin.class,
            ignoreFieldPaths = {"res.ignoreStr1", "res.ignoreList.str2"}
    )
    @Override
    public TestResponse testIgnore() {
        return TestResponse.builder()
                .number(1)
                .ignoreStr1("123")
                .ignoreList(new ArrayList<>(Arrays.asList(
                        TestIgnoreDTO.builder().str1("1").str2("2").build(),
                        TestIgnoreDTO.builder().str1("1").str2("2").build()
                )))
                .build();
    }

    /**
     * 测试比对限流场景。
     * <p>限制比对流量，比如只比3个调用，超过三次，后面所有的调用都不比对。
     */
    @Janus(
            methodId = "testCompareThrottling",
            compareType = CompareType.SYNC_COMPARE,
            isAsyncCompare = false,
            plugins = {CompareThrottlingJanusPlugin.class, CountCompare2JanusPlugin.class}
    )
    @Override
    public void testCompareThrottling(TestRequest request) {

    }
}
