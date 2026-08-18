package com.workworth.goals.api.dto;

import com.workworth.goals.application.GoalProgress;
import com.workworth.goals.persistence.Goal;

import java.math.BigDecimal;
import java.time.Instant;

public record GoalResponse(Long id, String title, BigDecimal targetAmount, String currencyCode, String status,
                           Instant createdAt, Instant updatedAt, Instant closedAt, GoalProgressResponse progress) {

    public static GoalResponse from(Goal goal, GoalProgress progress) {
        return new GoalResponse(goal.getId(), goal.getTitle(), goal.getTargetAmount(), goal.getCurrencyCode(),
            goal.getStatus().name(), goal.getCreatedAt(), goal.getUpdatedAt(), goal.getClosedAt(),
            progress == null ? null : GoalProgressResponse.from(progress));
    }
}
