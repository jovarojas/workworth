package com.workworth.workday.api.dto;
import jakarta.validation.constraints.NotNull; import java.time.Instant;
public record CreatePartialAbsenceRequest(@NotNull Instant startedAt, @NotNull Instant endedAt, String reason) { }
