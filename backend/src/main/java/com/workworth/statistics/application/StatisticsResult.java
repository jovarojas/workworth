package com.workworth.statistics.application;

import com.workworth.statistics.domain.StatisticsGranularity;

import java.time.LocalDate;
import java.util.List;

public record StatisticsResult(StatisticsGranularity granularity, LocalDate from, LocalDate to,
                               List<StatisticsPoint> points) {
}
