package com.workworth.earnings.api.dto;

import com.workworth.earnings.persistence.WorkdayEarning;
import com.workworth.earnings.application.EffectiveEarning;

import java.math.*;
import java.time.LocalDate;

public record EarningResponse(LocalDate localDate, String status, String unavailableReason, BigDecimal amount,
                              String currencyCode, long economicSeconds) {
    public static EarningResponse from(EffectiveEarning earning) {
        WorkdayEarning base = earning.base();
        return new EarningResponse(base.getLocalDate(), base.getStatus().name(),
            base.getUnavailableReason() == null ? null : base.getUnavailableReason().name(),
            earning.amount() == null ? null : earning.amount().setScale(2, RoundingMode.HALF_UP),
            base.getCurrencyCode(), earning.economicSeconds());
    }
}
