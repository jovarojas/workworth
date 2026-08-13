package com.workworth.statistics.application;

import com.workworth.common.money.MoneyRounding;
import com.workworth.earnings.application.EarningHistoryPage;
import com.workworth.earnings.application.EarningQueryService;
import com.workworth.earnings.application.EffectiveEarning;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.goals.application.GoalService;
import com.workworth.goals.domain.GoalStatus;
import com.workworth.goals.persistence.Goal;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.statistics.domain.StatisticsGranularity;
import com.workworth.statistics.exception.StatisticsCurrencyMismatchException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    static final int MAX_POINTS = 366;
    private static final int EARNINGS_PAGE_SIZE = 100;
    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3_600);

    private final EarningQueryService earnings;
    private final GoalService goals;
    private final ApplicationCurrencyProvider currency;
    private final Clock clock;
    private final ZoneId zone;

    public StatisticsService(EarningQueryService earnings, GoalService goals, ApplicationCurrencyProvider currency,
                             Clock clock, @Value("${workworth.time-zone:Europe/Madrid}") String zone) {
        this.earnings = earnings;
        this.goals = goals;
        this.currency = currency;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    public StatisticsResult statistics(StatisticsGranularity granularity, LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<EffectiveEarning> effectiveEarnings = allEffectiveEarnings();
        List<Goal> closedGoals = goals.history();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        DateRange requestedRange = resolveRange(from, to, effectiveEarnings, closedGoals, today);
        if (requestedRange == null) {
            return new StatisticsResult(granularity, from, to, List.of());
        }

        List<DateRange> buckets = buckets(granularity, requestedRange);
        if (buckets.size() > MAX_POINTS) {
            throw new IllegalArgumentException("The requested Statistics range exceeds the maximum of 366 points.");
        }
        return new StatisticsResult(granularity, from, to, buckets.stream()
            .map(bucket -> point(bucket, effectiveEarnings, closedGoals))
            .toList());
    }

    private StatisticsPoint point(DateRange bucket, List<EffectiveEarning> allEarnings, List<Goal> closedGoals) {
        List<EffectiveEarning> periodEarnings = allEarnings.stream()
            .filter(earning -> bucket.includes(earning.base().getLocalDate()))
            .toList();
        long effectiveSeconds = periodEarnings.stream().mapToLong(EffectiveEarning::economicSeconds).sum();
        StatisticsValue workedHours = StatisticsValue.available(hours(effectiveSeconds));
        StatisticsMoney totalEarnings = totalEarnings(periodEarnings);
        StatisticsMoney averageHourlyEarnings = averageHourlyEarnings(periodEarnings, totalEarnings, effectiveSeconds);
        int completedGoals = (int) closedGoals.stream()
            .filter(goal -> goal.getStatus() == GoalStatus.COMPLETED)
            .filter(goal -> bucket.includes(LocalDate.ofInstant(goal.getClosedAt(), zone)))
            .count();
        return new StatisticsPoint(bucket.startDate(), bucket.endDate(), workedHours, averageHourlyEarnings,
            totalEarnings, StatisticsCount.available(completedGoals));
    }

    private StatisticsMoney totalEarnings(List<EffectiveEarning> periodEarnings) {
        if (periodEarnings.stream().anyMatch(earning -> earning.base().getStatus() == EarningStatus.UNAVAILABLE
            || earning.amount() == null)) {
            return StatisticsMoney.unavailable();
        }
        String expectedCurrency = currency.currentCurrency().name();
        boolean inconsistentCurrency = periodEarnings.stream()
            .anyMatch(earning -> !expectedCurrency.equals(earning.base().getCurrencyCode()));
        if (inconsistentCurrency) {
            throw new StatisticsCurrencyMismatchException(
                "An effective earning currency does not match the global application currency.");
        }
        BigDecimal total = periodEarnings.stream().map(EffectiveEarning::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return StatisticsMoney.available(MoneyRounding.money(total), expectedCurrency);
    }

    private StatisticsMoney averageHourlyEarnings(List<EffectiveEarning> periodEarnings, StatisticsMoney totalEarnings,
                                                  long effectiveSeconds) {
        if (totalEarnings.amount() == null || effectiveSeconds == 0) {
            return StatisticsMoney.unavailable();
        }
        BigDecimal internalTotal = periodEarnings.stream().map(EffectiveEarning::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal hourlyAmount = internalTotal.multiply(SECONDS_PER_HOUR)
            .divide(BigDecimal.valueOf(effectiveSeconds), MoneyRounding.MONEY_SCALE, RoundingMode.HALF_UP);
        return StatisticsMoney.available(hourlyAmount, totalEarnings.currencyCode());
    }

    private BigDecimal hours(long effectiveSeconds) {
        return BigDecimal.valueOf(effectiveSeconds).divide(SECONDS_PER_HOUR, MoneyRounding.MONEY_SCALE,
            MoneyRounding.ROUNDING_MODE);
    }

    private List<EffectiveEarning> allEffectiveEarnings() {
        List<EffectiveEarning> result = new ArrayList<>();
        for (int page = 0; ; page++) {
            EarningHistoryPage history = earnings.history(page, EARNINGS_PAGE_SIZE);
            result.addAll(history.items());
            if (!history.hasNext()) {
                return result;
            }
        }
    }

    private DateRange resolveRange(LocalDate from, LocalDate to, List<EffectiveEarning> effectiveEarnings,
                                   List<Goal> closedGoals, LocalDate today) {
        if (from != null) {
            return new DateRange(from, to.plusDays(1));
        }
        LocalDate firstEarning = effectiveEarnings.stream().map(earning -> earning.base().getLocalDate())
            .min(Comparator.naturalOrder()).orElse(null);
        LocalDate firstCompletedGoal = closedGoals.stream()
            .filter(goal -> goal.getStatus() == GoalStatus.COMPLETED)
            .map(goal -> LocalDate.ofInstant(goal.getClosedAt(), zone))
            .min(Comparator.naturalOrder()).orElse(null);
        LocalDate earliest = java.util.stream.Stream.of(firstEarning, firstCompletedGoal)
            .filter(java.util.Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
        return earliest == null ? null : new DateRange(earliest, today.plusDays(1));
    }

    private List<DateRange> buckets(StatisticsGranularity granularity, DateRange range) {
        List<DateRange> buckets = new ArrayList<>();
        LocalDate start = bucketStart(granularity, range.startDate());
        while (start.isBefore(range.endDate())) {
            LocalDate end = next(granularity, start);
            buckets.add(new DateRange(start, end));
            start = end;
        }
        return buckets;
    }

    private LocalDate bucketStart(StatisticsGranularity granularity, LocalDate date) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    private LocalDate next(StatisticsGranularity granularity, LocalDate start) {
        return switch (granularity) {
            case DAY -> start.plusDays(1);
            case WEEK -> start.plusWeeks(1);
            case MONTH -> start.plusMonths(1);
            case YEAR -> start.plusYears(1);
        };
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if ((from == null) != (to == null) || (from != null && from.isAfter(to))) {
            throw new IllegalArgumentException("Statistics from and to dates must be supplied together in ascending order.");
        }
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
        boolean includes(LocalDate date) {
            return !date.isBefore(startDate) && date.isBefore(endDate);
        }
    }
}
