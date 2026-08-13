package com.workworth.statistics.api.dto;

import com.workworth.statistics.application.StatisticsResult;

import java.time.LocalDate;
import java.util.List;

public record StatisticsResponse(String granularity, LocalDate from, LocalDate to, List<StatisticsPointResponse> points) {

    public static StatisticsResponse from(StatisticsResult result) {
        return new StatisticsResponse(result.granularity().name(), result.from(), result.to(),
            result.points().stream().map(StatisticsPointResponse::from).toList());
    }
}
