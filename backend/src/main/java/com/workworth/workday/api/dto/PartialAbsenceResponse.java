package com.workworth.workday.api.dto;

import com.workworth.workday.persistence.PartialAbsence;

import java.time.Instant;

public record PartialAbsenceResponse(Long id, Instant startedAt, Instant endedAt, String reason) {
    public static PartialAbsenceResponse from(PartialAbsence a) {
        return new PartialAbsenceResponse(a.getId(), a.getStartedAt(), a.getEndedAt(), a.getReason());
    }
}
