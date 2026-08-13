package com.workworth.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.workworth.earnings.application.EarningHistoryPage;
import com.workworth.earnings.application.EarningQueryService;
import com.workworth.earnings.application.EffectiveEarning;
import com.workworth.earnings.domain.EarningCorrectionCause;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.earnings.persistence.EarningCorrection;
import com.workworth.earnings.persistence.WorkdayEarning;
import com.workworth.goals.application.GoalService;
import com.workworth.goals.persistence.Goal;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.statistics.domain.StatisticAvailability;
import com.workworth.statistics.domain.StatisticsGranularity;
import com.workworth.statistics.exception.StatisticsCurrencyMismatchException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {

    private final EarningQueryService earnings = mock(EarningQueryService.class);
    private final GoalService goals = mock(GoalService.class);
    private final ApplicationCurrencyProvider currency = mock(ApplicationCurrencyProvider.class);
    private StatisticsService service;

    @BeforeEach
    void setUp() {
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.EUR);
        history(List.of());
        when(goals.history()).thenReturn(List.of());
        service = new StatisticsService(earnings, goals, currency,
            Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneOffset.UTC), "Europe/Madrid");
    }

    @Test
    void aggregatesCorrectedEffectiveEarningsAndEconomicTimeByDay() {
        WorkdayEarning base = earning(LocalDate.of(2026, 8, 10), EarningStatus.AVAILABLE, 3_600, "10.00", "EUR");
        EarningCorrection correction = new EarningCorrection(base, 1L, null, 1, EarningCorrectionCause.WORKDAY_CANCELLED,
            3_600, 7_200, new BigDecimal("10.00"), new BigDecimal("25.00"), Instant.EPOCH);
        history(List.of(new EffectiveEarning(base, correction)));

        StatisticsPoint point = service.statistics(StatisticsGranularity.DAY,
            LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10)).points().get(0);

        assertThat(point.workedHours().status()).isEqualTo(StatisticAvailability.AVAILABLE);
        assertThat(point.workedHours().value()).isEqualByComparingTo("2.00");
        assertThat(point.totalEarnings().amount()).isEqualByComparingTo("25.00");
        assertThat(point.averageHourlyEarnings().amount()).isEqualByComparingTo("12.50");
    }

    @Test
    void usesLocalDailyWeeklyMonthlyAndYearlyBoundaries() {
        history(List.of(effective(LocalDate.of(2026, 1, 1), EarningStatus.AVAILABLE, 3_600, "10.00", "EUR")));

        var day = service.statistics(StatisticsGranularity.DAY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
        var week = service.statistics(StatisticsGranularity.WEEK, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
        var month = service.statistics(StatisticsGranularity.MONTH, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
        var year = service.statistics(StatisticsGranularity.YEAR, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertThat(day.points().get(0).startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(week.points().get(0).startDate()).isEqualTo(LocalDate.of(2025, 12, 29));
        assertThat(month.points().get(0).startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(year.points().get(0).startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void preservesValidZeroAndMakesAverageUnavailableWithoutHours() {
        StatisticsPoint point = service.statistics(StatisticsGranularity.DAY,
            LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10)).points().get(0);

        assertThat(point.workedHours().value()).isEqualByComparingTo("0.00");
        assertThat(point.totalEarnings().status()).isEqualTo(StatisticAvailability.AVAILABLE);
        assertThat(point.totalEarnings().amount()).isEqualByComparingTo("0.00");
        assertThat(point.averageHourlyEarnings().status()).isEqualTo(StatisticAvailability.UNAVAILABLE);
        assertThat(point.completedGoals().count()).isZero();
    }

    @Test
    void keepsHoursAndGoalCompletionAvailableWhenEarningsAreUnavailable() {
        history(List.of(effective(LocalDate.of(2026, 8, 10), EarningStatus.UNAVAILABLE, 3_600, null, null)));
        Goal completed = new Goal("Viaje", new BigDecimal("100.00"), "EUR", Instant.EPOCH);
        completed.complete(Instant.parse("2026-08-10T10:00:00Z"));
        when(goals.history()).thenReturn(List.of(completed));

        StatisticsPoint point = service.statistics(StatisticsGranularity.DAY,
            LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10)).points().get(0);

        assertThat(point.workedHours().value()).isEqualByComparingTo("1.00");
        assertThat(point.totalEarnings().status()).isEqualTo(StatisticAvailability.UNAVAILABLE);
        assertThat(point.totalEarnings().amount()).isNull();
        assertThat(point.averageHourlyEarnings().status()).isEqualTo(StatisticAvailability.UNAVAILABLE);
        assertThat(point.completedGoals().count()).isEqualTo(1);
    }

    @Test
    void countsOnlyCompletedGoalsByClosedAt() {
        Goal completed = new Goal("Viaje", new BigDecimal("100.00"), "EUR", Instant.EPOCH);
        completed.complete(Instant.parse("2026-08-10T10:00:00Z"));
        Goal cancelled = new Goal("Curso", new BigDecimal("100.00"), "EUR", Instant.EPOCH);
        cancelled.cancel(Instant.parse("2026-08-10T10:00:00Z"));
        when(goals.history()).thenReturn(List.of(completed, cancelled));

        StatisticsPoint point = service.statistics(StatisticsGranularity.WEEK,
            LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10)).points().get(0);

        assertThat(point.completedGoals().count()).isEqualTo(1);
    }

    @Test
    void usesTheGlobalUsdCurrencyWithoutConversion() {
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.USD);
        history(List.of(effective(LocalDate.of(2026, 8, 10), EarningStatus.AVAILABLE, 3_600, "20.00", "USD")));

        StatisticsPoint point = service.statistics(StatisticsGranularity.DAY,
            LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10)).points().get(0);

        assertThat(point.totalEarnings().amount()).isEqualByComparingTo("20.00");
        assertThat(point.totalEarnings().currencyCode()).isEqualTo("USD");
    }

    @Test
    void rejectsAnInconsistentCurrencyInsteadOfConvertingIt() {
        history(List.of(effective(LocalDate.of(2026, 8, 10), EarningStatus.AVAILABLE, 3_600, "20.00", "USD")));

        assertThatThrownBy(() -> service.statistics(StatisticsGranularity.DAY,
            LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10)))
            .isInstanceOf(StatisticsCurrencyMismatchException.class);
    }

    @Test
    void returnsNoPointsForAnUnboundedEmptyHistory() {
        var result = service.statistics(StatisticsGranularity.MONTH, null, null);

        assertThat(result.points()).isEmpty();
    }

    @Test
    void rejectsRequestsOverTheMaximumPointLimitAndIncompleteRanges() {
        assertThatThrownBy(() -> service.statistics(StatisticsGranularity.DAY,
            LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.statistics(StatisticsGranularity.DAY, LocalDate.of(2026, 1, 1), null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private void history(List<EffectiveEarning> items) {
        when(earnings.history(0, 100)).thenReturn(new EarningHistoryPage(items, 0, 100, items.size(), 1, false, false));
    }

    private EffectiveEarning effective(LocalDate date, EarningStatus status, long seconds, String amount, String currencyCode) {
        return new EffectiveEarning(earning(date, status, seconds, amount, currencyCode), null);
    }

    private WorkdayEarning earning(LocalDate date, EarningStatus status, long seconds, String amount, String currencyCode) {
        return new WorkdayEarning(1L, date, status, seconds, amount == null ? null : new BigDecimal(amount), 1L,
            null, null, null, 12, currencyCode, null, null, Instant.EPOCH);
    }
}
