package com.workworth.salary.application;

import com.workworth.common.money.MoneyRounding;
import com.workworth.common.schedule.StandardEconomicHoursProvider;
import com.workworth.salary.domain.IncomeSource;
import com.workworth.salary.domain.MonthlySalaryRate;
import com.workworth.salary.exception.SalaryConfigurationIncompleteException;
import com.workworth.salary.exception.SalaryRateUnavailableException;
import com.workworth.salary.persistence.SalaryProfile;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class MonthlySalaryRateService {

    private final SalaryProfileService salaryProfileService;
    private final ObjectProvider<StandardEconomicHoursProvider> standardEconomicHoursProvider;
    private final Clock clock;

    public MonthlySalaryRateService(SalaryProfileService salaryProfileService,
                                    ObjectProvider<StandardEconomicHoursProvider> standardEconomicHoursProvider,
                                    Clock clock) {
        this.salaryProfileService = salaryProfileService;
        this.standardEconomicHoursProvider = standardEconomicHoursProvider;
        this.clock = clock;
    }

    public MonthlySalaryRate getRate(YearMonth month) {
        SalaryProfile profile = salaryProfileService.findEffectiveProfile(month);
        if (profile.getNetMonthlyReal() == null) {
            throw new SalaryConfigurationIncompleteException(
                    "A real monthly net income is required until a fiscal estimator is implemented.");
        }

        StandardEconomicHoursProvider provider = standardEconomicHoursProvider.getIfAvailable();
        if (provider == null) {
            throw new SalaryRateUnavailableException(
                    "The standard work calendar is not available until SPEC 002 is implemented.");
        }

        BigDecimal standardHours = provider.getStandardEconomicHours(month, clock.getZone());
        if (standardHours == null || standardHours.signum() <= 0) {
            throw new SalaryRateUnavailableException("No standard economic hours are available for " + month + ".");
        }

        BigDecimal hourlyRate = profile.getNetMonthlyReal()
                .divide(standardHours, MoneyRounding.RATE_SCALE, MoneyRounding.ROUNDING_MODE);

        return new MonthlySalaryRate(
                month,
                profile.getId(),
                IncomeSource.NET_MONTHLY_REAL,
                profile.getNetMonthlyReal(),
                profile.getNetMonthlyReal().multiply(BigDecimal.valueOf(profile.getPayPeriods())),
                profile.getPayPeriods(),
                standardHours,
                hourlyRate,
                profile.getCurrencyCode());
    }
}
