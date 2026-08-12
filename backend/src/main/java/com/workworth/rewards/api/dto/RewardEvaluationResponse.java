package com.workworth.rewards.api.dto;

import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.rewards.application.RewardEvaluation;

import java.math.BigDecimal;

public record RewardEvaluationResponse(Long rewardId, String context, boolean evaluable, String outcome,
                                       BigDecimal availableAmount, BigDecimal price, String currencyCode,
                                       BigDecimal surplus, BigDecimal shortfall) {

    public static RewardEvaluationResponse from(RewardEvaluation evaluation) {
        return new RewardEvaluationResponse(evaluation.rewardId(), evaluation.context().name(), evaluation.evaluable(),
            evaluation.outcome() == null ? null : evaluation.outcome().name(), evaluation.availableAmount(),
            evaluation.price(), evaluation.currencyCode(), evaluation.surplus(), evaluation.shortfall());
    }
}
