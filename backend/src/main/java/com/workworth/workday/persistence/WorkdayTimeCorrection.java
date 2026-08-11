package com.workworth.workday.persistence;

import com.workworth.workday.domain.WorkdayTimeCorrectionCause;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

@Getter @Entity @Table(name = "workday_time_corrections")
public class WorkdayTimeCorrection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workday_id", nullable = false) private Workday workday;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private WorkdayTimeCorrectionCause cause;
    @Column(name = "previous_economic_seconds", nullable = false) private long previousEconomicSeconds;
    @Column(name = "new_economic_seconds", nullable = false) private long newEconomicSeconds;
    @Column(name = "corrected_at", nullable = false) private Instant correctedAt;
    protected WorkdayTimeCorrection() { }
    public WorkdayTimeCorrection(Workday workday, WorkdayTimeCorrectionCause cause, long previous, long updated, Instant at) { this.workday=workday; this.cause=cause; previousEconomicSeconds=previous; newEconomicSeconds=updated; correctedAt=at; }
}
