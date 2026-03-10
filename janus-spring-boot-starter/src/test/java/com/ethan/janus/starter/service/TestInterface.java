package com.ethan.janus.starter.service;

import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;

public interface TestInterface {

    TestResponse testAsyncCompare1(TestRequest request);

    TestResponse testAsyncCompare2(TestRequest request);

    TestResponse testSyncCompare(TestRequest request);

    TestResponse testRollbackOne(TestRequest request);

    TestResponse testRollbackAll(TestRequest request);

    TestResponse testIgnore();

    void testCompareThrottling(TestRequest request);
}
