package com.workworth.workday.api.dto;

import com.workworth.workday.persistence.MealBreak;
import com.workworth.workday.persistence.PartialAbsence;
import com.workworth.workday.persistence.Workday;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record WorkdayResponse(
    Long id,
    LocalDate localDate,
    String timeZone,
    String status,
    LocalTime scheduledStart,
    LocalTime scheduledEnd,
    long maximumEconomicSeconds,
    long economicSeconds,
    List<MealBreakResponse> mealBreaks,
    List<PartialAbsenceResponse> partialAbsences) {

    public static WorkdayResponse from(Workday workday, long economicSeconds,
                                       List<MealBreak> mealBreaks, List<PartialAbsence> partialAbsences) {
        return new WorkdayResponse(
            workday.getId(),
            workday.getLocalDate(),
            workday.getTimeZone(),
            workday.getStatus().name(),
            workday.getScheduledStart(),
            workday.getScheduledEnd(),
            workday.getMaximumEconomicSeconds(),
            economicSeconds,
            mealBreaks.stream().map(MealBreakResponse::from).toList(),
            partialAbsences.stream().map(PartialAbsenceResponse::from).toList());
    }
}
