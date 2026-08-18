package com.workworth.goals.api.dto;

import com.workworth.goals.application.GoalProgress;

import java.math.BigDecimal;

public record GoalProgressResponse(boolean evaluable, BigDecimal progressAmount, BigDecimal remainingAmount,
                                   BigDecimal progressPercentage, Boolean reached) {

    public static GoalProgressResponse from(GoalProgress progress) {
        return new GoalProgressResponse(progress.evaluable(), progress.progressAmount(), progress.remainingAmount(),
            progress.progressPercentage(), progress.reached());
    }
}
