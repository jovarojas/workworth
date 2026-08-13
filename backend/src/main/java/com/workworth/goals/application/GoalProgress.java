package com.workworth.goals.application;

import java.math.BigDecimal;

public record GoalProgress(boolean evaluable, BigDecimal progressAmount, BigDecimal remainingAmount,
                           BigDecimal progressPercentage, Boolean reached) {

    public static GoalProgress unavailable() {
        return new GoalProgress(false, null, null, null, null);
    }
}
