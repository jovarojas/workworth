package com.workworth.workday.api.dto;

import com.workworth.workday.persistence.MealBreak;

import java.time.Instant;

public record MealBreakResponse(Long id, Instant startedAt, Instant endedAt, boolean endedAutomatically) {
    public static MealBreakResponse from(MealBreak b) {
        return new MealBreakResponse(b.getId(), b.getStartedAt(), b.getEndedAt(), b.isEndedAutomatically());
    }
}
