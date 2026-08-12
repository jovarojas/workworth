package com.workworth.earnings.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EarningPeriodSummary(EarningPeriod period, EarningStatus status, LocalDate startDate, LocalDate endDate,
                                   BigDecimal internalAmount, BigDecimal publicAmount, String currencyCode) {
}
