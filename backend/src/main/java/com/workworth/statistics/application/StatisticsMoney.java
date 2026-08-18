package com.workworth.statistics.application;

import com.workworth.statistics.domain.StatisticAvailability;

import java.math.BigDecimal;

public record StatisticsMoney(StatisticAvailability status, BigDecimal amount, String currencyCode) {

    public static StatisticsMoney available(BigDecimal amount, String currencyCode) {
        return new StatisticsMoney(StatisticAvailability.AVAILABLE, amount, currencyCode);
    }

    public static StatisticsMoney unavailable() {
        return new StatisticsMoney(StatisticAvailability.UNAVAILABLE, null, null);
    }
}
