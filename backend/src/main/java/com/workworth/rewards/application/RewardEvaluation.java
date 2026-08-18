package com.workworth.rewards.application;

import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.rewards.domain.RewardOutcome;

import java.math.BigDecimal;

public record RewardEvaluation(Long rewardId, EarningPeriod context, boolean evaluable, RewardOutcome outcome,
                               BigDecimal availableAmount, BigDecimal price, String currencyCode,
                               BigDecimal surplus, BigDecimal shortfall) {
}
