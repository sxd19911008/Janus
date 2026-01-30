package com.ethan.janus.starter.service;

import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestResponse;

public interface TestInterface {

    TestResponse testMethod(TestRequest request);
}
