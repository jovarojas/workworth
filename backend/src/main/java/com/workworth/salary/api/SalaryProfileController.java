package com.workworth.salary.api;

import com.workworth.salary.api.dto.CreateSalaryProfileRequest;
import com.workworth.salary.api.dto.CurrentSalaryProfileResponse;
import com.workworth.salary.api.dto.EstimatorStatusResponse;
import com.workworth.salary.api.dto.MonthlySalaryRateResponse;
import com.workworth.salary.api.dto.SalaryProfileHistoryResponse;
import com.workworth.salary.api.dto.SalaryProfileResponse;
import com.workworth.salary.application.MonthlySalaryRateService;
import com.workworth.salary.application.SalaryProfileService;
import com.workworth.salary.domain.EstimatorStatus;
import com.workworth.salary.domain.MonthlySalaryRate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SalaryProfileController {

    private final SalaryProfileService salaryProfileService;
    private final MonthlySalaryRateService monthlySalaryRateService;
    private final Clock clock;

    public SalaryProfileController(SalaryProfileService salaryProfileService,
                                   MonthlySalaryRateService monthlySalaryRateService,
                                   Clock clock) {
        this.salaryProfileService = salaryProfileService;
        this.monthlySalaryRateService = monthlySalaryRateService;
        this.clock = clock;
    }

    @PostMapping("/salary-profiles")
    public ResponseEntity<SalaryProfileResponse> create(@Valid @RequestBody CreateSalaryProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salaryProfileService.create(request));
    }

    @GetMapping("/salary-profiles/current")
    public ResponseEntity<CurrentSalaryProfileResponse> getCurrent(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth requestedMonth = month == null ? YearMonth.now(clock) : month;
        return ResponseEntity.ok(new CurrentSalaryProfileResponse(
                requestedMonth,
                salaryProfileService.getCurrent(requestedMonth)));
    }

    @GetMapping("/salary-profiles")
    public ResponseEntity<SalaryProfileHistoryResponse> getHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(salaryProfileService.getHistory(page, size));
    }

    @GetMapping("/salary-rates/{month}")
    public ResponseEntity<MonthlySalaryRateResponse> getRate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        MonthlySalaryRate rate = monthlySalaryRateService.getRate(month);
        return ResponseEntity.ok(new MonthlySalaryRateResponse(
                rate.month(),
                rate.incomeSource(),
                rate.monthlyNetIncome(),
                rate.standardEconomicHours(),
                rate.hourlyNetRate(),
                rate.currencyCode()));
    }

    @GetMapping("/salary-estimator/status")
    public ResponseEntity<EstimatorStatusResponse> getEstimatorStatus(
            @RequestParam(required = false) Integer year) {
        int fiscalYear = year == null ? YearMonth.now(clock).getYear() : year;
        return ResponseEntity.ok(new EstimatorStatusResponse(
                fiscalYear,
                EstimatorStatus.NOT_IMPLEMENTED,
                List.of("Fiscal estimator implementation")));
    }
}
