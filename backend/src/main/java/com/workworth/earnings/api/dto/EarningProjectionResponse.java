package com.workworth.earnings.api.dto;

import com.workworth.earnings.domain.EarningProjection;

import java.math.*;
import java.time.LocalDate;

public record EarningProjectionResponse(LocalDate localDate, String status, long economicSeconds, BigDecimal amount,
                                        String currencyCode, String unavailableReason) {
    public static EarningProjectionResponse from(EarningProjection projection) {
        return new EarningProjectionResponse(projection.localDate(), projection.status().name(), projection.economicSeconds(), projection.rawAmount() == null ? null : projection.rawAmount().setScale(2, RoundingMode.HALF_UP), projection.currencyCode(), projection.unavailableReason() == null ? null : projection.unavailableReason().name());
    }
}
