package com.workworth.earnings.application;

import com.workworth.earnings.domain.EarningStatus;
import com.workworth.earnings.domain.EarningUnavailableReason;
import com.workworth.earnings.persistence.WorkdayEarning;
import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.salary.application.MonthlySalaryRateService;
import com.workworth.salary.exception.SalaryConfigurationIncompleteException;
import com.workworth.salary.exception.SalaryProfileNotFoundException;
import com.workworth.salary.exception.SalaryRateUnavailableException;
import com.workworth.workday.application.WorkdayService;
import com.workworth.workday.domain.WorkdayStatus;
import com.workworth.workday.persistence.Workday;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EarningMaterializationService {
    private final WorkdayEarningRepository earnings;
    private final WorkdayService workdays;
    private final MonthlySalaryRateService rates;
    private final Clock clock;
    public EarningMaterializationService(WorkdayEarningRepository earnings, WorkdayService workdays,
                                         MonthlySalaryRateService rates, Clock clock) {
        this.earnings = earnings; this.workdays = workdays; this.rates = rates; this.clock = clock;
    }
    @Transactional public WorkdayEarning materialize(Workday day) {
        if (day.getStatus() != WorkdayStatus.COMPLETED) throw new IllegalStateException("Only completed workdays can materialize historical earnings.");
        return earnings.findByWorkdayId(day.getId()).orElseGet(() -> create(day));
    }
    private WorkdayEarning create(Workday day) {
        long seconds = workdays.time(day);
        try {
            var rate = rates.getRate(YearMonth.from(day.getLocalDate()));
            BigDecimal amount = rate.hourlyNetRate().multiply(BigDecimal.valueOf(seconds)).divide(BigDecimal.valueOf(3600), 12, RoundingMode.HALF_UP);
            return earnings.save(new WorkdayEarning(day.getId(), day.getLocalDate(), EarningStatus.AVAILABLE, seconds, amount, rate.salaryProfileId(), rate.incomeSource(), rate.monthlyNetIncome(), rate.annualNetIncome(), rate.payPeriods(), rate.currencyCode(), rate.standardEconomicHours(), rate.hourlyNetRate(), clock.instant()));
        } catch (SalaryProfileNotFoundException exception) { return unavailable(day, seconds, EarningUnavailableReason.SALARY_PROFILE_NOT_FOUND); }
        catch (SalaryConfigurationIncompleteException exception) { return unavailable(day, seconds, EarningUnavailableReason.SALARY_CONFIGURATION_INCOMPLETE); }
        catch (SalaryRateUnavailableException exception) { return unavailable(day, seconds, EarningUnavailableReason.SALARY_RATE_UNAVAILABLE); }
    }
    private WorkdayEarning unavailable(Workday day, long seconds, EarningUnavailableReason reason) {
        return earnings.save(new WorkdayEarning(day.getId(), day.getLocalDate(), EarningStatus.UNAVAILABLE, reason, seconds, null, null, null, null, null, 0, null, null, null, clock.instant()));
    }
}
