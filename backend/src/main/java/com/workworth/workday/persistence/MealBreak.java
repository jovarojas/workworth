package com.workworth.workday.persistence;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;

@Getter
@Entity
@Table(name = "meal_breaks")
public class MealBreak {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workday_id", nullable = false)
    private Workday workday;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "ended_at")
    private Instant endedAt;
    @Column(name = "ended_automatically", nullable = false)
    private boolean endedAutomatically;

    protected MealBreak() {
    }

    public MealBreak(Workday workday, Instant startedAt) {
        this.workday = workday;
        this.startedAt = startedAt;
    }

    public void end(Instant endedAt, boolean automatic) {
        this.endedAt = endedAt;
        this.endedAutomatically = automatic;
    }

    public void amend(Instant startedAt, Instant endedAt) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.endedAutomatically = false;
    }
}
