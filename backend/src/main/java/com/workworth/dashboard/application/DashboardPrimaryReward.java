package com.workworth.dashboard.application;

import com.workworth.rewards.application.RewardEvaluation;
import com.workworth.rewards.persistence.Reward;

public record DashboardPrimaryReward(Reward reward, RewardEvaluation evaluation) {
}
