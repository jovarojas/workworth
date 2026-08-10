package com.workworth.salary.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSalaryProfileRequest(
        @NotNull LocalDate effectiveFrom,
        @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal grossAnnual,
        @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal netMonthlyReal,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currencyCode,
        @NotNull @Min(12) @Max(12) Integer payPeriods) {
}
