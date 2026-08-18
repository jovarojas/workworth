package com.workworth.dashboard.api.dto;

import com.workworth.dashboard.application.DashboardMotivation;
public record DashboardMotivationResponse(String state, DashboardPrimaryRewardResponse primaryReward,
                                          DashboardCombinationResponse combination) {

    public static DashboardMotivationResponse from(DashboardMotivation motivation) {
        return new DashboardMotivationResponse(motivation.state().name(), motivation.primaryReward() == null ? null
            : DashboardPrimaryRewardResponse.from(motivation.primaryReward()), motivation.combination() == null ? null
                : DashboardCombinationResponse.from(motivation.combination()));
    }
}
