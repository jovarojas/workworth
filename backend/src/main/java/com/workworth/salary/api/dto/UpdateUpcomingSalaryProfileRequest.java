package com.workworth.salary.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Edits the already-scheduled, not-yet-effective salary change exposed by
 * GET /salary-profiles/upcoming. Only the amount can change; the effective date stays fixed to
 * the first day of the month the user originally scheduled it for.
 */
public record UpdateUpcomingSalaryProfileRequest(
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal netMonthlyReal) {
}
