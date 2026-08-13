package com.workworth.dashboard.api.dto;

import com.workworth.rewards.application.RewardCombination;

import java.math.BigDecimal;
import java.util.List;

public record DashboardCombinationResponse(String context, BigDecimal availableAmount, BigDecimal totalPrice,
                                           String currencyCode, List<DashboardRewardResponse> rewards) {

    public static DashboardCombinationResponse from(RewardCombination combination) {
        return new DashboardCombinationResponse(combination.context().name(), combination.availableAmount(),
            combination.totalPrice(), combination.currencyCode(), combination.rewards().stream()
                .map(DashboardRewardResponse::from).toList());
    }
}
