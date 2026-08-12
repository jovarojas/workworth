package com.workworth.earnings.application;

import com.workworth.earnings.persistence.EarningCorrection;
import com.workworth.earnings.persistence.WorkdayEarning;
import java.math.BigDecimal;

public record EffectiveEarning(WorkdayEarning base, EarningCorrection latestCorrection) {

    public long economicSeconds() {
        return latestCorrection == null ? base.getEconomicSeconds() : latestCorrection.getNewEconomicSeconds();
    }

    public BigDecimal amount() {
        return latestCorrection == null ? base.getRawAmount() : latestCorrection.getNewAmount();
    }
}
