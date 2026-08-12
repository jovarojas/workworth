package com.workworth.rewards.api.dto;

import com.workworth.rewards.persistence.Reward;

import java.math.BigDecimal;
import java.time.Instant;

public record RewardResponse(Long id, String name, int quantity, BigDecimal price, String currencyCode,
                             String status, String lastReachedContext, Instant createdAt, Instant updatedAt) {

    public static RewardResponse from(Reward reward) {
        return new RewardResponse(reward.getId(), reward.getName(), reward.getQuantity(), reward.getPrice(),
            reward.getCurrencyCode(), reward.getStatus().name(), reward.getLastReachedContext() == null ? null
                : reward.getLastReachedContext().name(), reward.getCreatedAt(), reward.getUpdatedAt());
    }
}
