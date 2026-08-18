package com.workworth.workday.application;

import com.workworth.common.schedule.StandardEconomicHoursProvider;
import com.workworth.workday.domain.WorkdaySchedule;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

@Service
public class StandardEconomicHoursService implements StandardEconomicHoursProvider {
    @Override
    public BigDecimal getStandardEconomicHours(YearMonth month, ZoneId zoneId) {
        long seconds = month.atDay(1).datesUntil(month.plusMonths(1).atDay(1))
            .map(WorkdaySchedule::forDate)
            .flatMap(java.util.Optional::stream)
            .mapToLong(schedule -> schedule.maximumEconomicTime().getSeconds())
            .sum();
        return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600));
    }
}
