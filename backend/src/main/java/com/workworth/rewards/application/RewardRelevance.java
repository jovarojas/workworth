package com.workworth.rewards.application;

import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.rewards.domain.RewardOutcome;

import java.math.BigDecimal;

public record RewardRelevance(Long rewardId, boolean evaluable, EarningPeriod relevantContext,
                              EarningPeriod progressContext, RewardOutcome outcome, BigDecimal availableAmount,
                              BigDecimal price, String currencyCode, BigDecimal surplus, BigDecimal shortfall,
                              boolean newlyReached, EarningPeriod previousReachedContext) {
}
