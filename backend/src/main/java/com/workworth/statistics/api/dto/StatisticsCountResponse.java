package com.workworth.statistics.api.dto;

import com.workworth.statistics.application.StatisticsCount;

public record StatisticsCountResponse(String status, Integer count) {

    public static StatisticsCountResponse from(StatisticsCount count) {
        return new StatisticsCountResponse(count.status().name(), count.count());
    }
}
