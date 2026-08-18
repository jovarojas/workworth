package com.workworth.rewards.api.dto;

import com.workworth.rewards.application.RewardRelevance;

import java.math.BigDecimal;

public record RewardRelevanceResponse(Long rewardId, boolean evaluable, String relevantContext, String progressContext,
                                      String outcome, BigDecimal availableAmount, BigDecimal price, String currencyCode,
                                      BigDecimal surplus, BigDecimal shortfall, boolean newlyReached,
                                      String previousReachedContext) {

    public static RewardRelevanceResponse from(RewardRelevance relevance) {
        return new RewardRelevanceResponse(relevance.rewardId(), relevance.evaluable(),
            relevance.relevantContext() == null ? null : relevance.relevantContext().name(),
            relevance.progressContext() == null ? null : relevance.progressContext().name(),
            relevance.outcome() == null ? null : relevance.outcome().name(), relevance.availableAmount(),
            relevance.price(), relevance.currencyCode(), relevance.surplus(), relevance.shortfall(),
            relevance.newlyReached(), relevance.previousReachedContext() == null ? null
                : relevance.previousReachedContext().name());
    }
}
