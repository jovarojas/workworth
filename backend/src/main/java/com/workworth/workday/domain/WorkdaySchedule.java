package com.workworth.workday.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public record WorkdaySchedule(ScheduleVariant variant, LocalTime start, LocalTime end,
                              Duration maximumEconomicTime) {

    public static Optional<WorkdaySchedule> forDate(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return Optional.empty();
        }
        if (date.getMonthValue() == 7 || date.getMonthValue() == 8) {
            return Optional.of(new WorkdaySchedule(ScheduleVariant.SUMMER, LocalTime.of(8, 0),
                    LocalTime.of(15, 0), Duration.ofHours(7)));
        }
        if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return Optional.of(new WorkdaySchedule(ScheduleVariant.NORMAL, LocalTime.of(8, 0),
                    LocalTime.of(15, 0), Duration.ofHours(7)));
        }
        return Optional.of(new WorkdaySchedule(ScheduleVariant.NORMAL, LocalTime.of(8, 0),
                LocalTime.of(17, 0), Duration.ofHours(8)));
    }
}
