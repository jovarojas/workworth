package com.workworth.workday.application;

import com.workworth.workday.persistence.*;

import java.time.*;
import java.util.*;

import org.springframework.stereotype.Component;

@Component
public class EconomicTimeCalculator {
    public long calculate(Workday workday, List<MealBreak> breaks, List<PartialAbsence> absences, Instant now) {
        if (workday.getStatus().name().equals("CANCELLED")) return 0;
        ZoneId zone = ZoneId.of(workday.getTimeZone());
        Instant start = workday.getLocalDate().atTime(workday.getScheduledStart()).atZone(zone).toInstant();
        Instant end = workday.getLocalDate().atTime(workday.getScheduledEnd()).atZone(zone).toInstant();
        Instant capped = now.isBefore(end) ? now : end;
        if (!capped.isAfter(start)) return 0;
        long excluded = 0;
        for (MealBreak b : breaks)
            excluded += overlap(start, capped, b.getStartedAt(), b.getEndedAt() == null ? capped : b.getEndedAt());
        for (PartialAbsence a : absences) excluded += overlap(start, capped, a.getStartedAt(), a.getEndedAt());
        return Math.max(0, Math.min(workday.getMaximumEconomicSeconds(), Duration.between(start, capped).getSeconds() - excluded));
    }

    private long overlap(Instant start, Instant end, Instant from, Instant to) {
        Instant s = from.isAfter(start) ? from : start;
        Instant e = to.isBefore(end) ? to : end;
        return e.isAfter(s) ? Duration.between(s, e).getSeconds() : 0;
    }
}
