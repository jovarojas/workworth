package com.workworth.earnings.application;

import com.workworth.earnings.domain.*;
import com.workworth.salary.application.MonthlySalaryRateService;
import com.workworth.salary.exception.*;
import com.workworth.workday.application.WorkdayService;
import java.math.*;
import java.time.*;
import org.springframework.stereotype.Service;

@Service public class ActiveEarningProjectionService {
    private final WorkdayService workdays; private final MonthlySalaryRateService rates; private final Clock clock;
    public ActiveEarningProjectionService(WorkdayService workdays, MonthlySalaryRateService rates, Clock clock) { this.workdays = workdays; this.rates = rates; this.clock = clock; }
    public EarningProjection current() {
        LocalDate date = LocalDate.now(clock); var day = workdays.reconcile(date); long seconds = workdays.time(day);
        try { var rate = rates.getRate(YearMonth.from(date)); return new EarningProjection(date, EarningStatus.AVAILABLE, seconds, rate.hourlyNetRate().multiply(BigDecimal.valueOf(seconds)).divide(BigDecimal.valueOf(3600), 12, RoundingMode.HALF_UP), rate.currencyCode(), null); }
        catch (SalaryProfileNotFoundException exception) { return unavailable(date, seconds, EarningUnavailableReason.SALARY_PROFILE_NOT_FOUND); }
        catch (SalaryConfigurationIncompleteException exception) { return unavailable(date, seconds, EarningUnavailableReason.SALARY_CONFIGURATION_INCOMPLETE); }
        catch (SalaryRateUnavailableException exception) { return unavailable(date, seconds, EarningUnavailableReason.SALARY_RATE_UNAVAILABLE); }
    }
    private EarningProjection unavailable(LocalDate date, long seconds, EarningUnavailableReason reason) { return new EarningProjection(date, EarningStatus.UNAVAILABLE, seconds, null, null, reason); }
}
