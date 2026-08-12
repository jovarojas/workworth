package com.workworth.earnings.api.dto;

import com.workworth.earnings.persistence.EarningCorrection;

import java.math.*;
import java.time.Instant;

public record EarningCorrectionResponse(int sequence, String cause, long previousEconomicSeconds,
                                        long newEconomicSeconds, BigDecimal previousAmount, BigDecimal newAmount,
                                        Instant correctedAt) {
    public static EarningCorrectionResponse from(EarningCorrection c) {
        return new EarningCorrectionResponse(c.getSequence(), c.getCause().name(), c.getPreviousEconomicSeconds(), c.getNewEconomicSeconds(), money(c.getPreviousAmount()), money(c.getNewAmount()), c.getCorrectedAt());
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }
}
