package com.workworth.rewards.api.dto;

import com.workworth.rewards.application.RewardCombinationRelevance;

public record RewardCombinationRelevanceResponse(boolean evaluable, RewardCombinationResponse combination) {

    public static RewardCombinationRelevanceResponse from(RewardCombinationRelevance relevance) {
        return new RewardCombinationRelevanceResponse(relevance.evaluable(), relevance.combination() == null ? null
            : RewardCombinationResponse.from(relevance.combination()));
    }
}
