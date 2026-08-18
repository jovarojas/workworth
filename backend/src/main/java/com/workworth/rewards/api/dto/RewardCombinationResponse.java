package com.workworth.rewards.api.dto;

import com.workworth.rewards.application.RewardCombination;

import java.math.BigDecimal;
import java.util.List;

public record RewardCombinationResponse(String context, boolean evaluable, BigDecimal availableAmount,
                                        BigDecimal totalPrice, String currencyCode, List<RewardResponse> rewards) {

    public static RewardCombinationResponse from(RewardCombination combination) {
        return new RewardCombinationResponse(combination.context().name(), combination.evaluable(),
            combination.availableAmount(), combination.totalPrice(), combination.currencyCode(),
            combination.rewards().stream().map(RewardResponse::from).toList());
    }
}
