package com.ethan.janus.starter.service;

import com.ethan.janus.core.annotation.Janus;
import com.ethan.janus.core.constants.CompareType;
import com.ethan.janus.starter.annotation.TestAnnotation;
import com.ethan.janus.starter.dao.TestRollbackMapper;
import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;
import com.ethan.janus.starter.dto.TestRollbackEntity;
import com.ethan.janus.starter.plugins.ExecuteTimeJanusPlugin;
import com.ethan.janus.starter.plugins.TestAnnotationJanusPlugin;
import com.ethan.janus.starter.plugins.TestRollbackOneQueryDataJanusPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Service
public class PrimaryService implements TestInterface {

    @Autowired
    private TestRollbackMapper testRollbackMapper;

    @Janus(
            methodId = "testSyncCompare",
            compareType = CompareType.SYNC_COMPARE,
            isAsyncCompare = false,
            businessKey = "buildKey(#request.key, 'qqq')",
            plugins = {TestAnnotationJanusPlugin.class, ExecuteTimeJanusPlugin.class}
    )
    @TestAnnotation(value = "Archimonde")
    @Override
    public TestResponse testSyncCompare(TestRequest request) {
        if ("1".equals(request.getKey())) {
            return new TestResponse(1);
        } else if ("2".equals(request.getKey())) {
            return new TestResponse(2);
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new TestResponse(0);
    }

    @Janus(
            methodId = "testRollbackOne",
            compareType = CompareType.SYNC_ROLLBACK_ONE_COMPARE,
            isAsyncCompare = false,
            businessKey = "#request.key",
            plugins = {TestRollbackOneQueryDataJanusPlugin.class, ExecuteTimeJanusPlugin.class}
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
        } else if ("b".equals(key)) {
            testRollbackMapper.updateByKey("pre", 20);
        } else if ("err".equals(key)) {
            testRollbackMapper.deleteByKey("pre");
        }
        testRollbackMapper.selectNumByKey("exist");
        return new TestResponse(0);
    }
}
