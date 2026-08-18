package com.workworth.rewards.application;

import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.rewards.persistence.Reward;

import java.math.BigDecimal;
import java.util.List;

public record RewardCombination(EarningPeriod context, boolean evaluable, BigDecimal availableAmount,
                                BigDecimal totalPrice, String currencyCode, List<Reward> rewards) {
}
