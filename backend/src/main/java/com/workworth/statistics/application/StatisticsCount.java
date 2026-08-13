package com.workworth.statistics.application;

import com.workworth.statistics.domain.StatisticAvailability;

public record StatisticsCount(StatisticAvailability status, Integer count) {

    public static StatisticsCount available(int count) {
        return new StatisticsCount(StatisticAvailability.AVAILABLE, count);
    }
}
