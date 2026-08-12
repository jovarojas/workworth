package com.workworth.earnings.api;

import com.workworth.earnings.api.dto.EarningCorrectionResponse;
import com.workworth.earnings.api.dto.EarningHistoryResponse;
import com.workworth.earnings.api.dto.EarningPeriodResponse;
import com.workworth.earnings.api.dto.EarningProjectionResponse;
import com.workworth.earnings.api.dto.EarningResponse;
import com.workworth.earnings.application.ActiveEarningProjectionService;
import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.application.EarningQueryService;
import com.workworth.earnings.domain.EarningPeriod;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/earnings")
public class EarningsController {

    private final ActiveEarningProjectionService projections;
    private final EarningPeriodService periods;
    private final EarningQueryService queries;

    public EarningsController(ActiveEarningProjectionService projections, EarningPeriodService periods,
                              EarningQueryService queries) {
        this.projections = projections;
        this.periods = periods;
        this.queries = queries;
    }

    @GetMapping("/current/projection")
    public ResponseEntity<EarningProjectionResponse> projection() {
        return ResponseEntity.ok(EarningProjectionResponse.from(projections.current()));
    }

    @GetMapping("/periods/{context}")
    public ResponseEntity<EarningPeriodResponse> period(@PathVariable EarningPeriod context) {
        return ResponseEntity.ok(EarningPeriodResponse.from(periods.summarize(context)));
    }

    @GetMapping("/workdays/{date}")
    public ResponseEntity<EarningResponse> workday(@PathVariable LocalDate date) {
        return ResponseEntity.ok(EarningResponse.from(queries.byDate(date)));
    }

    @GetMapping("/history")
    public ResponseEntity<EarningHistoryResponse> history(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters.");
        }
        return ResponseEntity.ok(EarningHistoryResponse.from(queries.history(page, size)));
    }

    @GetMapping("/workdays/{date}/corrections")
    public ResponseEntity<List<EarningCorrectionResponse>> corrections(@PathVariable LocalDate date) {
        return ResponseEntity.ok(queries.corrections(date).stream().map(EarningCorrectionResponse::from).toList());
    }
}
