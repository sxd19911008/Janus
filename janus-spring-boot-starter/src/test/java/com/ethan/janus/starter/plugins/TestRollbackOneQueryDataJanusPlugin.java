package com.ethan.janus.starter.plugins;

import com.ethan.janus.core.dto.JanusContext;
import com.ethan.janus.core.plugin.JanusPlugin;
import com.ethan.janus.starter.dao.TestRollbackMapper;
import com.ethan.janus.starter.dto.TestRequest;
import com.ethan.janus.starter.dto.TestRollbackEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TestRollbackOneQueryDataJanusPlugin implements JanusPlugin {

    @Autowired
    private TestRollbackMapper testRollbackMapper;

    @Override
    public int getOrder() {
        return 10000;
    }

    @Override
    public void afterPrimaryExecute(JanusContext context) {
        List<TestRollbackEntity> queryRes = getQueryRes(context);
        context.setPrimaryQueryRes(queryRes);
    }

    @Override
    public void afterSecondaryExecute(JanusContext context) {
        List<TestRollbackEntity> queryRes = getQueryRes(context);
        context.setSecondaryQueryRes(queryRes);
    }

    private List<TestRollbackEntity> getQueryRes(JanusContext context) {
        Object[] args = context.getArgs();
        TestRequest request = (TestRequest) args[0];
        String reqKey = request.getKey();
        List<String> keyList = new ArrayList<>(Arrays.asList("exist", "delete"));
        keyList.add(reqKey);
        List<TestRollbackEntity> queryRes = new ArrayList<>();
        for (String key : keyList) {
            List<TestRollbackEntity> queryList = testRollbackMapper.selectByKey(key);
            queryRes.addAll(queryList);
        }
        return queryRes.stream()
                .sorted(Comparator.comparing(TestRollbackEntity::getTblKey)
                        .thenComparing(TestRollbackEntity::getTblNum))
                .collect(Collectors.toList());
    }
}
