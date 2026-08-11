package com.workworth.workday.persistence;

import com.workworth.workday.domain.ScheduleVariant;
import com.workworth.workday.domain.WorkdayStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "workdays")
public class Workday {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "local_date", nullable = false, unique = true) private LocalDate localDate;
    @Column(name = "time_zone", nullable = false, length = 64) private String timeZone;
    @Enumerated(EnumType.STRING) @Column(name = "schedule_variant", nullable = false, length = 16) private ScheduleVariant scheduleVariant;
    @Column(name = "scheduled_start", nullable = false) private LocalTime scheduledStart;
    @Column(name = "scheduled_end", nullable = false) private LocalTime scheduledEnd;
    @Column(name = "maximum_economic_seconds", nullable = false) private long maximumEconomicSeconds;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WorkdayStatus status;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "cancellation_reason", length = 500) private String cancellationReason;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Workday() { }
    public Workday(LocalDate date, String zone, ScheduleVariant variant, LocalTime start, LocalTime end, long maxSeconds, Instant now) {
        localDate=date; timeZone=zone; scheduleVariant=variant; scheduledStart=start; scheduledEnd=end;
        maximumEconomicSeconds=maxSeconds; status=WorkdayStatus.SCHEDULED; createdAt=now; updatedAt=now;
    }
    public void changeStatus(WorkdayStatus value, Instant now) { status=value; updatedAt=now; }
    public void cancel(String reason, Instant now) { status=WorkdayStatus.CANCELLED; cancellationReason=reason; cancelledAt=now; updatedAt=now; }
}
