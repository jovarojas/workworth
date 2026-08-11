package com.workworth.salary.application;

import com.workworth.common.money.MoneyRounding;
import com.workworth.salary.api.dto.SalaryProfileResponse;
import com.workworth.salary.domain.EstimatorStatus;
import com.workworth.salary.domain.IncomeSource;
import com.workworth.salary.persistence.SalaryProfile;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class SalaryProfileMapper {

    public SalaryProfileResponse toResponse(SalaryProfile profile) {
        BigDecimal netAnnualReal = profile.getNetMonthlyReal() == null
                ? null
                : MoneyRounding.money(profile.getNetMonthlyReal().multiply(BigDecimal.valueOf(profile.getPayPeriods())));
        IncomeSource source = profile.getNetMonthlyReal() == null
                ? IncomeSource.UNAVAILABLE
                : IncomeSource.NET_MONTHLY_REAL;

        return new SalaryProfileResponse(
                profile.getId(),
                profile.getEffectiveFrom(),
                profile.getGrossAnnual(),
                profile.getNetMonthlyReal(),
                netAnnualReal,
                profile.getCurrencyCode(),
                profile.getPayPeriods(),
                source,
                EstimatorStatus.NOT_IMPLEMENTED);
    }
}
