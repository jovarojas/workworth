package com.workworth.rewards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.earnings.domain.EarningPeriodSummary;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.rewards.domain.RewardOutcome;
import com.workworth.rewards.persistence.Reward;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RewardEvaluationServiceTest {

    private final RewardService rewards = mock(RewardService.class);
    private final EarningPeriodService periods = mock(EarningPeriodService.class);
    private final ApplicationCurrencyProvider currency = mock(ApplicationCurrencyProvider.class);
    private final Reward reward = new Reward("Auriculares", 1, new BigDecimal("120.00"), "EUR", Instant.EPOCH);
    private RewardEvaluationService service;

    @BeforeEach
    void setUp() {
        when(rewards.pending(4L)).thenReturn(reward);
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.EUR);
        service = new RewardEvaluationService(rewards, periods, currency,
            Clock.fixed(Instant.parse("2026-08-12T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void evaluatesEqualLowerAndHigherAmountsWithoutChangingTheExplicitEvaluationMarker() {
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(available(EarningPeriod.TODAY, "120.00"));
        when(periods.summarize(EarningPeriod.WEEK)).thenReturn(available(EarningPeriod.WEEK, "85.00"));
        when(periods.summarize(EarningPeriod.MONTH)).thenReturn(available(EarningPeriod.MONTH, "150.00"));

        var equal = service.evaluate(4L, EarningPeriod.TODAY);
        var lower = service.evaluate(4L, EarningPeriod.WEEK);
        var higher = service.evaluate(4L, EarningPeriod.MONTH);

        assertThat(equal.outcome()).isEqualTo(RewardOutcome.AFFORDABLE);
        assertThat(equal.surplus()).isZero();
        assertThat(lower.outcome()).isEqualTo(RewardOutcome.SHORTFALL);
        assertThat(lower.shortfall()).isEqualByComparingTo("35.00");
        assertThat(higher.surplus()).isEqualByComparingTo("30.00");
        assertThat(reward.getLastReachedContext()).isNull();
    }

    @Test
    void selectsTheFirstAffordableContextAndDetectsAnImprovement() {
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(available(EarningPeriod.TODAY, "20.00"));
        when(periods.summarize(EarningPeriod.WEEK)).thenReturn(available(EarningPeriod.WEEK, "80.00"));
        when(periods.summarize(EarningPeriod.MONTH)).thenReturn(available(EarningPeriod.MONTH, "120.00"));
        when(periods.summarize(EarningPeriod.ALL_TIME)).thenReturn(available(EarningPeriod.ALL_TIME, "200.00"));

        var month = service.relevance(4L);

        assertThat(month.relevantContext()).isEqualTo(EarningPeriod.MONTH);
        assertThat(month.newlyReached()).isTrue();
        assertThat(reward.getLastReachedContext()).isEqualTo(EarningPeriod.MONTH);

        when(periods.summarize(EarningPeriod.WEEK)).thenReturn(available(EarningPeriod.WEEK, "120.00"));
        var week = service.relevance(4L);

        assertThat(week.relevantContext()).isEqualTo(EarningPeriod.WEEK);
        assertThat(week.previousReachedContext()).isEqualTo(EarningPeriod.MONTH);
        assertThat(week.newlyReached()).isTrue();
        assertThat(reward.getLastReachedContext()).isEqualTo(EarningPeriod.WEEK);
    }

    @Test
    void usesTheFirstEvaluableContextForProgressAndSkipsUnavailableContexts() {
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(unavailable(EarningPeriod.TODAY));
        when(periods.summarize(EarningPeriod.WEEK)).thenReturn(available(EarningPeriod.WEEK, "85.00"));
        when(periods.summarize(EarningPeriod.MONTH)).thenReturn(unavailable(EarningPeriod.MONTH));
        when(periods.summarize(EarningPeriod.ALL_TIME)).thenReturn(available(EarningPeriod.ALL_TIME, "100.00"));

        var relevance = service.relevance(4L);

        assertThat(relevance.evaluable()).isTrue();
        assertThat(relevance.relevantContext()).isNull();
        assertThat(relevance.progressContext()).isEqualTo(EarningPeriod.WEEK);
        assertThat(relevance.outcome()).isEqualTo(RewardOutcome.SHORTFALL);
        assertThat(relevance.shortfall()).isEqualByComparingTo("35.00");
    }

    @Test
    void returnsNonEvaluableWhenAllContextsAreUnavailable() {
        for (EarningPeriod context : EarningPeriod.values()) {
            when(periods.summarize(context)).thenReturn(unavailable(context));
        }

        var relevance = service.relevance(4L);

        assertThat(relevance.evaluable()).isFalse();
        assertThat(relevance.outcome()).isNull();
        assertThat(relevance.availableAmount()).isNull();
        assertThat(relevance.price()).isEqualByComparingTo("120.00");
        assertThat(relevance.currencyCode()).isEqualTo("EUR");
    }

    @Test
    void consumesTheEffectiveCorrectedAmountProvidedByEarnings() {
        when(periods.summarize(EarningPeriod.MONTH)).thenReturn(available(EarningPeriod.MONTH, "125.00"));

        var evaluation = service.evaluate(4L, EarningPeriod.MONTH);

        assertThat(evaluation.outcome()).isEqualTo(RewardOutcome.AFFORDABLE);
        assertThat(evaluation.surplus()).isEqualByComparingTo("5.00");
    }

    private EarningPeriodSummary available(EarningPeriod context, String amount) {
        BigDecimal value = new BigDecimal(amount);
        return new EarningPeriodSummary(context, EarningStatus.AVAILABLE, LocalDate.now(), LocalDate.now().plusDays(1),
            value, value, "EUR");
    }

    private EarningPeriodSummary unavailable(EarningPeriod context) {
        return new EarningPeriodSummary(context, EarningStatus.UNAVAILABLE, LocalDate.now(), LocalDate.now().plusDays(1),
            null, null, null);
    }
}
