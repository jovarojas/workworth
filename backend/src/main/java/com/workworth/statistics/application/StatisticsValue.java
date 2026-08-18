package com.workworth.statistics.application;

import com.workworth.statistics.domain.StatisticAvailability;

import java.math.BigDecimal;

public record StatisticsValue(StatisticAvailability status, BigDecimal value) {

    public static StatisticsValue available(BigDecimal value) {
        return new StatisticsValue(StatisticAvailability.AVAILABLE, value);
    }

    public static StatisticsValue unavailable() {
        return new StatisticsValue(StatisticAvailability.UNAVAILABLE, null);
    }
}
