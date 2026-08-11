package com.workworth.workday.application;

import com.workworth.workday.domain.*;
import com.workworth.workday.exception.*;
import com.workworth.workday.persistence.*;

import java.time.*;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkdayService {
    private final WorkdayRepository workdays;
    private final MealBreakRepository breaks;
    private final PartialAbsenceRepository absences;
    private final WorkdayTimeCorrectionRepository corrections;
    private final EconomicTimeCalculator calculator;
    private final Clock clock;
    private final ZoneId zone;
    private final ApplicationEventPublisher events;

    public WorkdayService(WorkdayRepository w, MealBreakRepository b, PartialAbsenceRepository a, WorkdayTimeCorrectionRepository c, EconomicTimeCalculator calculator, Clock clock, @Value("${workworth.time-zone:Europe/Madrid}") String zone, ApplicationEventPublisher events) {
        workdays = w;
        breaks = b;
        absences = a;
        corrections = c;
        this.calculator = calculator;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
        this.events = events;
    }

    @Transactional
    public Workday reconcile(LocalDate date) {
        Instant now = clock.instant();
        Optional<WorkdaySchedule> schedule = WorkdaySchedule.forDate(date);
        if (schedule.isEmpty()) throw new WorkdayNotFoundException("No standard workday exists for this date.");
        Workday day = workdays.findLockedByLocalDate(date).orElseGet(() -> {
            var s = schedule.get();
            return workdays.save(new Workday(date, zone.getId(), s.variant(), s.start(), s.end(), s.maximumEconomicTime().getSeconds(), now));
        });
        refresh(day, now);
        return day;
    }

    @Transactional
    public MealBreak startMealBreak(LocalDate date) {
        Workday d = reconcile(date);
        if (d.getStatus() != WorkdayStatus.ACTIVE)
            throw new WorkdayConflictException("A meal break can only start on an active workday.");
        if (breaks.findByWorkdayIdOrderByStartedAt(d.getId()).stream().anyMatch(x -> x.getEndedAt() == null))
            throw new WorkdayConflictException("A meal break is already open.");
        Instant now = clock.instant();
        validatePoint(d, now);
        MealBreak b = breaks.save(new MealBreak(d, now));
        d.changeStatus(WorkdayStatus.ON_MEAL_BREAK, now);
        return b;
    }

    @Transactional
    public MealBreak endMealBreak(LocalDate date, Long id) {
        Workday d = reconcile(date);
        MealBreak b = breaks.findById(id).filter(x -> x.getWorkday().getId().equals(d.getId())).orElseThrow(() -> new WorkdayNotFoundException("Meal break not found."));
        if (b.getEndedAt() != null) throw new WorkdayConflictException("Meal break is already closed.");
        Instant now = clock.instant();
        b.end(now, false);
        refresh(d, now);
        return b;
    }

    @Transactional
    public MealBreak amendHistoricalMealBreak(LocalDate date, Long id, Instant start, Instant end) {
        Workday d = reconcile(date);
        if (d.getStatus() != WorkdayStatus.COMPLETED)
            throw new WorkdayConflictException("Historical meal breaks can only be amended on completed workdays.");
        MealBreak b = breaks.findById(id).filter(x -> x.getWorkday().getId().equals(d.getId()))
                .orElseThrow(() -> new WorkdayNotFoundException("Meal break not found."));
        if (b.getEndedAt() == null) throw new WorkdayConflictException("An open meal break cannot be amended.");
        if (b.getStartedAt().equals(start) && b.getEndedAt().equals(end)) return b;

        long before = time(d);
        Instant previousStart = b.getStartedAt();
        Instant previousEnd = b.getEndedAt();
        boolean previouslyAutomatic = b.isEndedAutomatically();
        validateInterval(d, start, end, null, id);
        b.amend(start, end);
        recordMealBreakAmendment(d, before, time(d), b, previousStart, previousEnd, previouslyAutomatic);
        return b;
    }

    @Transactional
    public PartialAbsence addAbsence(LocalDate date, Instant start, Instant end, String reason) {
        Workday d = reconcile(date);
        long before = time(d);
        validateInterval(d, start, end, null, null);
        PartialAbsence a = absences.save(new PartialAbsence(d, start, end, reason));
        record(d, before, time(d), WorkdayTimeCorrectionCause.PARTIAL_ABSENCE_CHANGED);
        return a;
    }

    @Transactional
    public PartialAbsence updateAbsence(LocalDate date, Long id, Instant start, Instant end, String reason) {
        Workday d = reconcile(date);
        PartialAbsence a = absences.findById(id).filter(x -> x.getWorkday().getId().equals(d.getId())).orElseThrow(() -> new WorkdayNotFoundException("Partial absence not found."));
        long before = time(d);
        validateInterval(d, start, end, id, null);
        a.change(start, end, reason);
        record(d, before, time(d), WorkdayTimeCorrectionCause.PARTIAL_ABSENCE_CHANGED);
        return a;
    }

    @Transactional
    public void cancel(LocalDate date, String reason) {
        Workday d = reconcile(date);
        long before = time(d);
        d.cancel(reason, clock.instant());
        record(d, before, 0, WorkdayTimeCorrectionCause.WORKDAY_CANCELLED);
    }

    public long time(Workday d) {
        return calculator.calculate(d, breaks.findByWorkdayIdOrderByStartedAt(d.getId()), absences.findByWorkdayIdOrderByStartedAt(d.getId()), clock.instant());
    }

    private void refresh(Workday d, Instant now) {
        if (d.getStatus() == WorkdayStatus.CANCELLED) return;
        WorkdayStatus previousStatus = d.getStatus();
        Instant start = at(d, d.getScheduledStart()), end = at(d, d.getScheduledEnd());
        if (!now.isBefore(end)) {
            breaks.findByWorkdayIdOrderByStartedAt(d.getId()).stream().filter(x -> x.getEndedAt() == null).forEach(x -> x.end(end, true));
            d.changeStatus(WorkdayStatus.COMPLETED, now);
            if (previousStatus != WorkdayStatus.COMPLETED) {
                events.publishEvent(new WorkdayCompletedEvent(d.getId()));
            }
        } else if (now.isBefore(start)) d.changeStatus(WorkdayStatus.SCHEDULED, now);
        else if (breaks.findByWorkdayIdOrderByStartedAt(d.getId()).stream().anyMatch(x -> x.getEndedAt() == null))
            d.changeStatus(WorkdayStatus.ON_MEAL_BREAK, now);
        else d.changeStatus(WorkdayStatus.ACTIVE, now);
    }

    private void validatePoint(Workday d, Instant p) {
        validateInterval(d, p, p.plusSeconds(1), null, null);
    }

    private void validateInterval(Workday d, Instant start, Instant end, Long ignoredAbsenceId, Long ignoredMealBreakId) {
        if (!end.isAfter(start)) throw new WorkdayIntervalValidationException("Interval end must be after start.");
        if (d.getStatus() == WorkdayStatus.CANCELLED)
            throw new WorkdayIntervalValidationException("Cancelled workdays cannot have intervals.");
        if (start.isBefore(at(d, d.getScheduledStart())) || end.isAfter(at(d, d.getScheduledEnd())))
            throw new WorkdayIntervalValidationException("Interval must be inside the scheduled window.");
        boolean overlap = breaks.findByWorkdayIdOrderByStartedAt(d.getId()).stream().anyMatch(x ->
                (ignoredMealBreakId == null || !java.util.Objects.equals(x.getId(), ignoredMealBreakId))
                        && overlaps(start, end, x.getStartedAt(), x.getEndedAt() == null ? at(d, d.getScheduledEnd()) : x.getEndedAt()))
                || absences.findByWorkdayIdOrderByStartedAt(d.getId()).stream().anyMatch(x ->
                (ignoredAbsenceId == null || !java.util.Objects.equals(x.getId(), ignoredAbsenceId))
                        && overlaps(start, end, x.getStartedAt(), x.getEndedAt()));
        if (overlap) throw new WorkdayIntervalValidationException("Workday intervals cannot overlap.");
    }

    private boolean overlaps(Instant a, Instant b, Instant c, Instant d) {
        return a.isBefore(d) && c.isBefore(b);
    }

    private Instant at(Workday d, LocalTime t) {
        return d.getLocalDate().atTime(t).atZone(ZoneId.of(d.getTimeZone())).toInstant();
    }

    private void record(Workday d, long before, long after, WorkdayTimeCorrectionCause cause) {
        if (d.getStatus() == WorkdayStatus.COMPLETED || d.getStatus() == WorkdayStatus.CANCELLED) {
            WorkdayTimeCorrection correction = corrections.save(new WorkdayTimeCorrection(d, cause, before, after, clock.instant()));
            events.publishEvent(new WorkdayTimeCorrectionRegisteredEvent(correction.getId()));
        }
    }

    private void recordMealBreakAmendment(Workday d, long before, long after, MealBreak mealBreak,
                                           Instant previousStart, Instant previousEnd, boolean previouslyAutomatic) {
        WorkdayTimeCorrection correction = corrections.save(new WorkdayTimeCorrection(
                d, before, after, clock.instant(), mealBreak, previousStart, previousEnd,
                previouslyAutomatic, mealBreak.getStartedAt(), mealBreak.getEndedAt()));
        events.publishEvent(new WorkdayTimeCorrectionRegisteredEvent(correction.getId()));
    }
}
