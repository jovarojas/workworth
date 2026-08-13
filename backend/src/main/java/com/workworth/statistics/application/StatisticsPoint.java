package com.workworth.statistics.application;

import java.time.LocalDate;

public record StatisticsPoint(LocalDate startDate, LocalDate endDate, StatisticsValue workedHours,
                              StatisticsMoney averageHourlyEarnings, StatisticsMoney totalEarnings,
                              StatisticsCount completedGoals) {
}
