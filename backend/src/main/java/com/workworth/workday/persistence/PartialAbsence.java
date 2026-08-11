package com.workworth.workday.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

@Getter @Entity @Table(name = "partial_absences")
public class PartialAbsence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workday_id", nullable = false) private Workday workday;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "ended_at", nullable = false) private Instant endedAt;
    @Column(length = 500) private String reason;
    protected PartialAbsence() { }
    public PartialAbsence(Workday workday, Instant startedAt, Instant endedAt, String reason) { this.workday=workday; this.startedAt=startedAt; this.endedAt=endedAt; this.reason=reason; }
    public void change(Instant start, Instant end, String value) { startedAt=start; endedAt=end; reason=value; }
}
