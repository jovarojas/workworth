package com.workworth.dashboard.application;

import com.workworth.rewards.application.RewardCombination;

public record DashboardMotivation(DashboardMotivationState state, DashboardPrimaryReward primaryReward,
                                  RewardCombination combination) {
}
