package com.workworth.earnings.api.dto;

import com.workworth.earnings.persistence.WorkdayEarning;
import java.math.*;
import java.time.LocalDate;

public record EarningResponse(LocalDate localDate, String status, String unavailableReason, BigDecimal amount, String currencyCode, long economicSeconds) {
    public static EarningResponse from(WorkdayEarning earning) {
        return new EarningResponse(earning.getLocalDate(), earning.getStatus().name(), earning.getUnavailableReason() == null ? null : earning.getUnavailableReason().name(), earning.getRawAmount() == null ? null : earning.getRawAmount().setScale(2, RoundingMode.HALF_UP), earning.getCurrencyCode(), earning.getEconomicSeconds());
    }
}
