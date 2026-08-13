package com.workworth.statistics.api;

import com.workworth.statistics.api.dto.StatisticsResponse;
import com.workworth.statistics.application.StatisticsService;
import com.workworth.statistics.domain.StatisticsGranularity;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statistics;

    public StatisticsController(StatisticsService statistics) {
        this.statistics = statistics;
    }

    @GetMapping
    public ResponseEntity<StatisticsResponse> statistics(@RequestParam StatisticsGranularity granularity,
                                                          @RequestParam(required = false) LocalDate from,
                                                          @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(StatisticsResponse.from(statistics.statistics(granularity, from, to)));
    }
}
