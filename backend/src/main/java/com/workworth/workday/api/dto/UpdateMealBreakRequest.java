package com.workworth.workday.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateMealBreakRequest(@NotNull Instant startedAt, @NotNull Instant endedAt) {
}
