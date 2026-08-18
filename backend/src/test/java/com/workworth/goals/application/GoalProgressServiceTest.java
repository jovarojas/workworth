package com.workworth.goals.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.earnings.domain.EarningPeriodSummary;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.goals.exception.GoalCurrencyMismatchException;
import com.workworth.identity.application.TestUsers;
import com.workworth.goals.persistence.Goal;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.domain.ApplicationCurrency;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoalProgressServiceTest {

    private final EarningPeriodService periods = mock(EarningPeriodService.class);
    private final ApplicationCurrencyProvider currency = mock(ApplicationCurrencyProvider.class);
    private GoalProgressService service;

    @BeforeEach
    void setUp() {
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.EUR);
        service = new GoalProgressService(periods, currency);
    }

    @Test
    void resolvesZeroPartialAndCappedReachedProgressFromEffectiveAllTimeOnly() {
        Goal goal = goal("100.00", "EUR");
        when(periods.summarize(EarningPeriod.ALL_TIME)).thenReturn(available("0.00", "EUR"));

        GoalProgress zero = service.resolve(goal);

        assertThat(zero.evaluable()).isTrue();
        assertThat(zero.progressAmount()).isEqualByComparingTo("0.00");
        assertThat(zero.remainingAmount()).isEqualByComparingTo("100.00");
        assertThat(zero.progressPercentage()).isEqualByComparingTo("0.00");
        assertThat(zero.reached()).isFalse();

        when(periods.summarize(EarningPeriod.ALL_TIME)).thenReturn(available("25.00", "EUR"));
        GoalProgress partial = service.resolve(goal);

        assertThat(partial.remainingAmount()).isEqualByComparingTo("75.00");
        assertThat(partial.progressPercentage()).isEqualByComparingTo("25.00");
        assertThat(partial.reached()).isFalse();

        when(periods.summarize(EarningPeriod.ALL_TIME)).thenReturn(available("150.00", "EUR"));
        GoalProgress reached = service.resolve(goal);

        assertThat(reached.remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(reached.progressPercentage()).isEqualByComparingTo("100.00");
        assertThat(reached.reached()).isTrue();
    }

    @Test
    void returnsUnavailableProgressWithoutInventingAmounts() {
        when(periods.summarize(EarningPeriod.ALL_TIME)).thenReturn(new EarningPeriodSummary(EarningPeriod.ALL_TIME,
            EarningStatus.UNAVAILABLE, null, null, null, null, null));

        GoalProgress progress = service.resolve(goal("100.00", "EUR"));

        assertThat(progress.evaluable()).isFalse();
        assertThat(progress.progressAmount()).isNull();
        assertThat(progress.remainingAmount()).isNull();
        assertThat(progress.progressPercentage()).isNull();
        assertThat(progress.reached()).isNull();
    }

    @Test
    void rejectsInconsistentCurrenciesInsteadOfConvertingThem() {
        when(periods.summarize(EarningPeriod.ALL_TIME)).thenReturn(available("25.00", "USD"));

        assertThatThrownBy(() -> service.resolve(goal("100.00", "EUR")))
            .isInstanceOf(GoalCurrencyMismatchException.class);
    }

    private Goal goal(String targetAmount, String currencyCode) {
        return new Goal(TestUsers.user("test|goal-progress"), "Viaje", new BigDecimal(targetAmount),
            currencyCode, Instant.EPOCH);
    }

    private EarningPeriodSummary available(String amount, String currencyCode) {
        BigDecimal value = new BigDecimal(amount);
        return new EarningPeriodSummary(EarningPeriod.ALL_TIME, EarningStatus.AVAILABLE, null, null, value, value,
            currencyCode);
    }
}
