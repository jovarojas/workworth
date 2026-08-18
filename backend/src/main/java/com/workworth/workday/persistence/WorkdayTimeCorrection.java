package com.workworth.workday.persistence;

import com.workworth.workday.domain.WorkdayTimeCorrectionCause;
import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;

@Getter
@Entity
@Table(name = "workday_time_corrections")
public class WorkdayTimeCorrection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workday_id", nullable = false)
    private Workday workday;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkdayTimeCorrectionCause cause;
    @Column(name = "previous_economic_seconds", nullable = false)
    private long previousEconomicSeconds;
    @Column(name = "new_economic_seconds", nullable = false)
    private long newEconomicSeconds;
    @Column(name = "corrected_at", nullable = false)
    private Instant correctedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_break_id")
    private MealBreak mealBreak;
    @Column(name = "previous_break_started_at")
    private Instant previousBreakStartedAt;
    @Column(name = "previous_break_ended_at")
    private Instant previousBreakEndedAt;
    @Column(name = "new_break_started_at")
    private Instant newBreakStartedAt;
    @Column(name = "new_break_ended_at")
    private Instant newBreakEndedAt;
    @Column(name = "previous_break_ended_automatically")
    private Boolean previousBreakEndedAutomatically;

    protected WorkdayTimeCorrection() {
    }

    public WorkdayTimeCorrection(Workday workday, WorkdayTimeCorrectionCause cause, long previous, long updated, Instant at) {
        this.workday = workday;
        this.cause = cause;
        previousEconomicSeconds = previous;
        newEconomicSeconds = updated;
        correctedAt = at;
    }

    public WorkdayTimeCorrection(Workday workday, long previous, long updated, Instant at, MealBreak mealBreak,
                                 Instant previousStartedAt, Instant previousEndedAt, boolean previousEndedAutomatically,
                                 Instant newStartedAt, Instant newEndedAt) {
        this(workday, WorkdayTimeCorrectionCause.MEAL_BREAK_CHANGED, previous, updated, at);
        this.mealBreak = mealBreak;
        previousBreakStartedAt = previousStartedAt;
        previousBreakEndedAt = previousEndedAt;
        this.previousBreakEndedAutomatically = previousEndedAutomatically;
        newBreakStartedAt = newStartedAt;
        newBreakEndedAt = newEndedAt;
    }
}
