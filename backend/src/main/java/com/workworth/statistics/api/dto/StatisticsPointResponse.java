package com.workworth.statistics.api.dto;

import com.workworth.statistics.application.StatisticsPoint;

import java.time.LocalDate;

public record StatisticsPointResponse(LocalDate startDate, LocalDate endDate, StatisticsValueResponse workedHours,
                                      StatisticsMoneyResponse averageHourlyEarnings,
                                      StatisticsMoneyResponse totalEarnings,
                                      StatisticsCountResponse completedGoals) {

    public static StatisticsPointResponse from(StatisticsPoint point) {
        return new StatisticsPointResponse(point.startDate(), point.endDate(), StatisticsValueResponse.from(point.workedHours()),
            StatisticsMoneyResponse.from(point.averageHourlyEarnings()), StatisticsMoneyResponse.from(point.totalEarnings()),
            StatisticsCountResponse.from(point.completedGoals()));
    }
}
