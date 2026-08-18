package com.workworth.statistics.api.dto;

import com.workworth.statistics.application.StatisticsValue;

import java.math.BigDecimal;

public record StatisticsValueResponse(String status, BigDecimal value) {

    public static StatisticsValueResponse from(StatisticsValue value) {
        return new StatisticsValueResponse(value.status().name(), value.value());
    }
}
