package com.ethan.janus.core.compare;


import com.ethan.janus.core.constants.JanusConstants;
import com.ethan.janus.core.dto.BranchInfo;
import com.ethan.janus.core.dto.CompareRes;
import com.ethan.janus.core.utils.JanusUtils;
import com.ethan.janus.core.utils.JanusJsonUtils;

import java.util.Map;

import com.ethan.janus.core.dto.JanusContext;

/**
 * Janus 比对功能默认实现
 */
public class JanusCompareDefaultImpl implements JanusCompare {

    @Override
    public CompareRes compare(JanusContext context) {
        BranchInfo primaryBranch = context.getPrimaryBranch();
        BranchInfo secondaryBranch = context.getSecondaryBranch();

        CompareRes compareRes = new CompareRes();
        // 有异常，不需要比对具体的数据
        if (primaryBranch.isError() || secondaryBranch.isError()) {
            if (primaryBranch.isError() && secondaryBranch.isError()) {
                compareRes.setCompareStatus(JanusConstants.ALL_ERROR);
            } else if (primaryBranch.isError()) {
                compareRes.setCompareStatus(JanusConstants.PRIMARY_ERROR);
            } else {
                compareRes.setCompareStatus(JanusConstants.SECONDARY_ERROR);
            }
        } else {
            // 无异常，比对具体的数据
            Map<String, String> diffFieldMap = JanusJsonUtils.compareObj(
                    primaryBranch.getBranchRes(),
                    secondaryBranch.getBranchRes(),
                    context.getIgnoreFieldPaths()
            );
            if (JanusUtils.isEmpty(diffFieldMap)) {
                // diffFieldMap 为空代表比对通过，没有差异
                compareRes.setCompareStatus(JanusConstants.SUCCESS);
            } else {
                // diffFieldMap 有值，说明比对发现差异
                compareRes.setCompareStatus(JanusConstants.DIFFERENT);
                compareRes.setDiffFieldMap(diffFieldMap);
            }
        }
        return compareRes;
    }
}
