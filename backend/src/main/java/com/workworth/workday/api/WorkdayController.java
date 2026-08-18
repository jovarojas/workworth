package com.workworth.workday.api;

import com.workworth.workday.api.dto.*;
import com.workworth.workday.application.WorkdayService;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workdays")
@Validated
public class WorkdayController {
    private final WorkdayService service;
    private final Clock clock;

    public WorkdayController(WorkdayService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/{date}")
    public ResponseEntity<WorkdayResponse> get(@PathVariable LocalDate date) {
        var w = service.reconcile(date);
        return ResponseEntity.ok(WorkdayResponse.from(w, service.time(w), service.mealBreaks(w), service.partialAbsences(w)));
    }

    @GetMapping("/current")
    public ResponseEntity<WorkdayResponse> current() {
        var w = service.current();
        return ResponseEntity.ok(WorkdayResponse.from(w, service.time(w), service.mealBreaks(w), service.partialAbsences(w)));
    }

    @PostMapping("/{date}/meal-breaks/start")
    public ResponseEntity<MealBreakResponse> start(@PathVariable LocalDate date) {
        return ResponseEntity.ok(MealBreakResponse.from(service.startMealBreak(date)));
    }

    @PostMapping("/{date}/meal-breaks/{id}/end")
    public ResponseEntity<MealBreakResponse> end(@PathVariable LocalDate date, @PathVariable Long id) {
        return ResponseEntity.ok(MealBreakResponse.from(service.endMealBreak(date, id)));
    }

    @PutMapping("/{date}/meal-breaks/{id}")
    public ResponseEntity<MealBreakResponse> amend(@PathVariable LocalDate date, @PathVariable Long id, @RequestBody @jakarta.validation.Valid UpdateMealBreakRequest r) {
        return ResponseEntity.ok(MealBreakResponse.from(service.amendHistoricalMealBreak(date, id, r.startedAt(), r.endedAt())));
    }

    @PostMapping("/{date}/partial-absences")
    public ResponseEntity<PartialAbsenceResponse> absence(@PathVariable LocalDate date, @RequestBody @jakarta.validation.Valid CreatePartialAbsenceRequest r) {
        return ResponseEntity.ok(PartialAbsenceResponse.from(service.addAbsence(date, r.startedAt(), r.endedAt(), r.reason())));
    }

    @PutMapping("/{date}/partial-absences/{id}")
    public ResponseEntity<PartialAbsenceResponse> updateAbsence(@PathVariable LocalDate date, @PathVariable Long id, @RequestBody @jakarta.validation.Valid CreatePartialAbsenceRequest r) {
        return ResponseEntity.ok(PartialAbsenceResponse.from(service.updateAbsence(date, id, r.startedAt(), r.endedAt(), r.reason())));
    }

    @PostMapping("/{date}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable LocalDate date, @RequestBody(required = false) CancelWorkdayRequest r) {
        service.cancel(date, r == null ? null : r.reason());
        return ResponseEntity.noContent().build();
    }
}
