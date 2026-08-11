package com.workworth.salary.domain;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlySalaryRate(
        YearMonth month,
        Long salaryProfileId,
        IncomeSource incomeSource,
        BigDecimal monthlyNetIncome,
        BigDecimal annualNetIncome,
        int payPeriods,
        BigDecimal standardEconomicHours,
        BigDecimal hourlyNetRate,
        String currencyCode) {
}
