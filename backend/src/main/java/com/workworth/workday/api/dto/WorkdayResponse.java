package com.workworth.workday.api.dto;
import com.workworth.workday.persistence.Workday; import java.time.*;
public record WorkdayResponse(Long id, LocalDate localDate, String timeZone, String status, LocalTime scheduledStart, LocalTime scheduledEnd, long maximumEconomicSeconds, long economicSeconds) {
 public static WorkdayResponse from(Workday w,long seconds){return new WorkdayResponse(w.getId(),w.getLocalDate(),w.getTimeZone(),w.getStatus().name(),w.getScheduledStart(),w.getScheduledEnd(),w.getMaximumEconomicSeconds(),seconds);}
}
