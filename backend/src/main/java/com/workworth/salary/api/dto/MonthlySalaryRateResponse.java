package com.workworth.salary.api.dto;

import com.workworth.salary.domain.IncomeSource;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlySalaryRateResponse(
    YearMonth month,
    IncomeSource incomeSource,
    BigDecimal monthlyNetIncome,
    BigDecimal standardEconomicHours,
    BigDecimal hourlyNetRate,
    String currencyCode) {
}
