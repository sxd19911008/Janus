package com.ethan.janus.starter.service;

import com.ethan.janus.core.annotation.Secondary;
import com.ethan.janus.starter.dao.TestRollbackMapper;
import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;
import com.ethan.janus.starter.dto.TestRollbackEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Secondary
@Service
public class SecondaryService implements TestInterface {

    @Autowired
    private TestRollbackMapper testRollbackMapper;

    @Override
    public TestResponse testSyncCompare(TestRequest request) {
        if ("1".equals(request.getKey())) {
            return new TestResponse(1);
        } else if ("2".equals(request.getKey())) {
            return new TestResponse(3);
        }
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new TestResponse(0);
    }

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
        return new TestResponse(0);
    }
}
