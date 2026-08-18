package com.workworth.dashboard.api.dto;

import com.workworth.rewards.persistence.Reward;

import java.math.BigDecimal;

public record DashboardRewardResponse(Long id, String name, int quantity, BigDecimal price, String currencyCode,
                                      String status) {

    public static DashboardRewardResponse from(Reward reward) {
        return new DashboardRewardResponse(reward.getId(), reward.getName(), reward.getQuantity(), reward.getPrice(),
            reward.getCurrencyCode(), reward.getStatus().name());
    }
}
