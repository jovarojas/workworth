package com.workworth.dashboard.api.dto;

import com.workworth.dashboard.application.DashboardPrimaryReward;
import java.math.BigDecimal;

public record DashboardPrimaryRewardResponse(DashboardRewardResponse reward, boolean evaluable, String relevantContext,
                                             String progressContext, String outcome, BigDecimal availableAmount,
                                             BigDecimal surplus, BigDecimal shortfall) {

    public static DashboardPrimaryRewardResponse from(DashboardPrimaryReward primaryReward) {
        var evaluation = primaryReward.evaluation();
        boolean affordable = "AFFORDABLE".equals(evaluation.outcome().name());
        return new DashboardPrimaryRewardResponse(DashboardRewardResponse.from(primaryReward.reward()), evaluation.evaluable(),
            affordable ? evaluation.context().name() : null, affordable ? null : evaluation.context().name(),
            evaluation.outcome().name(),
            evaluation.availableAmount(), evaluation.surplus(), evaluation.shortfall());
    }
}
