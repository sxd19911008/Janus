package com.eredar.janus.core.compare;


import com.eredar.janus.core.constants.JanusConstants;
import com.eredar.janus.core.dto.BranchInfo;
import com.eredar.janus.core.dto.CompareRes;
import com.eredar.janus.core.dto.JanusContext;
import com.eredar.janus.core.utils.JanusJsonUtils;
import com.eredar.janus.core.utils.JanusUtils;

import java.util.Map;

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
            Map<String, String> diffFieldMap = JanusJsonUtils.compare(
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
