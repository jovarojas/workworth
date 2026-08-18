package com.workworth.salary.api.dto;

import com.workworth.salary.domain.EstimatorStatus;
import com.workworth.salary.domain.IncomeSource;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryProfileResponse(
    Long id,
    LocalDate effectiveFrom,
    BigDecimal grossAnnual,
    BigDecimal netMonthlyReal,
    BigDecimal netAnnualReal,
    String currencyCode,
    int payPeriods,
    IncomeSource activeIncomeSource,
    EstimatorStatus estimatorStatus) {
}
