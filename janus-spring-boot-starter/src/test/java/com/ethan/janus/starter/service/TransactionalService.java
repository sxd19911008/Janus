package com.ethan.janus.starter.service;

import com.ethan.janus.starter.dao.TestRollbackMapper;
import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalService {

    @Autowired
    private TestInterface testInterface;
    @Autowired
    private TestRollbackMapper testRollbackMapper;

    @Transactional(rollbackFor = Throwable.class)
    public TestResponse testRollbackOne(TestRequest request) {
        Integer existNum = testRollbackMapper.selectNumByKey("exist");
        testRollbackMapper.updateByKey("exist", existNum + 1);
        return testInterface.testRollbackOne(request);
    }
}
