package com.workworth.rewards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.earnings.domain.EarningPeriodSummary;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.rewards.domain.RewardStatus;
import com.workworth.rewards.persistence.Reward;
import com.workworth.rewards.persistence.RewardRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RewardCombinationServiceTest {

    @Test
    void selectsTwoOrMorePendingRewardsWithoutMultiplyingQuantityAndHonoursExclusions() {
        RewardRepository rewards = mock(RewardRepository.class);
        EarningPeriodService periods = mock(EarningPeriodService.class);
        ApplicationCurrencyProvider currency = mock(ApplicationCurrencyProvider.class);
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.EUR);
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(available(EarningPeriod.TODAY, "120.00"));
        Reward hamburgers = reward(1L, "Hamburguesas", 2, "30.00");
        Reward funkos = reward(2L, "Funkos", 2, "60.00");
        Reward book = reward(3L, "Libro", 1, "80.00");
        when(rewards.findAllByStatusOrderByIdAsc(RewardStatus.PENDING)).thenReturn(List.of(hamburgers, funkos, book));
        RewardCombinationService service = new RewardCombinationService(rewards, periods, currency);

        var combination = service.combination(EarningPeriod.TODAY, Set.of());
        var alternative = service.combination(EarningPeriod.TODAY, Set.of(2L));

        assertThat(combination.rewards()).containsExactly(hamburgers, funkos);
        assertThat(combination.totalPrice()).isEqualByComparingTo("90.00");
        assertThat(alternative.rewards()).containsExactly(hamburgers, book);
        assertThat(alternative.totalPrice()).isEqualByComparingTo("110.00");
    }

    @Test
    void returnsNoCombinationForUnavailableEarningsOrFewerThanTwoAffordableRewards() {
        RewardRepository rewards = mock(RewardRepository.class);
        EarningPeriodService periods = mock(EarningPeriodService.class);
        ApplicationCurrencyProvider currency = mock(ApplicationCurrencyProvider.class);
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.EUR);
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(new EarningPeriodSummary(EarningPeriod.TODAY,
            EarningStatus.UNAVAILABLE, LocalDate.now(), LocalDate.now().plusDays(1), null, null, null));
        RewardCombinationService service = new RewardCombinationService(rewards, periods, currency);

        var unavailable = service.combination(EarningPeriod.TODAY, Set.of());

        assertThat(unavailable.evaluable()).isFalse();
        assertThat(unavailable.rewards()).isEmpty();
    }

    @Test
    void selectsTodayAndStopsWhenTodayHasAValidCombination() {
        RewardRepository rewards = mock(RewardRepository.class);
        EarningPeriodService periods = mock(EarningPeriodService.class);
        ApplicationCurrencyProvider currency = currency();
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(available(EarningPeriod.TODAY, "90.00"));
        when(rewards.findAllByStatusOrderByIdAsc(RewardStatus.PENDING)).thenReturn(pendingRewards());
        RewardCombinationService service = new RewardCombinationService(rewards, periods, currency);

        var relevance = service.relevantCombination();

        assertThat(relevance.evaluable()).isTrue();
        assertThat(relevance.combination().context()).isEqualTo(EarningPeriod.TODAY);
        assertThat(relevance.combination().rewards()).hasSize(2);
        verify(periods).summarize(EarningPeriod.TODAY);
        verify(periods, never()).summarize(EarningPeriod.WEEK);
    }

    @Test
    void selectsTheFirstLaterContextWithAValidCombination() {
        RewardRepository rewards = mock(RewardRepository.class);
        EarningPeriodService periods = mock(EarningPeriodService.class);
        ApplicationCurrencyProvider currency = currency();
        when(rewards.findAllByStatusOrderByIdAsc(RewardStatus.PENDING)).thenReturn(pendingRewards());
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(available(EarningPeriod.TODAY, "50.00"));
        when(periods.summarize(EarningPeriod.WEEK)).thenReturn(available(EarningPeriod.WEEK, "90.00"));
        RewardCombinationService service = new RewardCombinationService(rewards, periods, currency);

        var relevance = service.relevantCombination();

        assertThat(relevance.combination().context()).isEqualTo(EarningPeriod.WEEK);
        verify(periods, never()).summarize(EarningPeriod.MONTH);
    }

    @Test
    void selectsMonthOrAllTimeOnlyAfterEarlierContextsHaveNoCombination() {
        RewardRepository rewards = mock(RewardRepository.class);
        EarningPeriodService periods = mock(EarningPeriodService.class);
        ApplicationCurrencyProvider currency = currency();
        when(rewards.findAllByStatusOrderByIdAsc(RewardStatus.PENDING)).thenReturn(pendingRewards());
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(available(EarningPeriod.TODAY, "20.00"));
        when(periods.summarize(EarningPeriod.WEEK)).thenReturn(available(EarningPeriod.WEEK, "50.00"));
        when(periods.summarize(EarningPeriod.MONTH)).thenReturn(available(EarningPeriod.MONTH, "90.00"));
        RewardCombinationService service = new RewardCombinationService(rewards, periods, currency);

        var month = service.relevantCombination();

        assertThat(month.combination().context()).isEqualTo(EarningPeriod.MONTH);
        verify(periods, never()).summarize(EarningPeriod.ALL_TIME);

        when(periods.summarize(EarningPeriod.MONTH)).thenReturn(available(EarningPeriod.MONTH, "50.00"));
        when(periods.summarize(EarningPeriod.ALL_TIME)).thenReturn(available(EarningPeriod.ALL_TIME, "90.00"));

        var allTime = service.relevantCombination();

        assertThat(allTime.combination().context()).isEqualTo(EarningPeriod.ALL_TIME);
    }

    @Test
    void skipsUnavailableContextsAndDistinguishesNoCombinationFromNoEvaluableContext() {
        RewardRepository rewards = mock(RewardRepository.class);
        EarningPeriodService periods = mock(EarningPeriodService.class);
        ApplicationCurrencyProvider currency = currency();
        when(rewards.findAllByStatusOrderByIdAsc(RewardStatus.PENDING)).thenReturn(pendingRewards());
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(unavailable(EarningPeriod.TODAY));
        when(periods.summarize(EarningPeriod.WEEK)).thenReturn(available(EarningPeriod.WEEK, "90.00"));
        RewardCombinationService service = new RewardCombinationService(rewards, periods, currency);

        var afterUnavailableToday = service.relevantCombination();

        assertThat(afterUnavailableToday.evaluable()).isTrue();
        assertThat(afterUnavailableToday.combination().context()).isEqualTo(EarningPeriod.WEEK);

        when(periods.summarize(EarningPeriod.WEEK)).thenReturn(unavailable(EarningPeriod.WEEK));
        when(periods.summarize(EarningPeriod.MONTH)).thenReturn(unavailable(EarningPeriod.MONTH));
        when(periods.summarize(EarningPeriod.ALL_TIME)).thenReturn(unavailable(EarningPeriod.ALL_TIME));

        var allUnavailable = service.relevantCombination();

        assertThat(allUnavailable.evaluable()).isFalse();
        assertThat(allUnavailable.combination()).isNull();
    }

    @Test
    void returnsEvaluableWithoutACombinationWhenNoTwoPendingRewardsFitAndDoesNotPersist() {
        RewardRepository rewards = mock(RewardRepository.class);
        EarningPeriodService periods = mock(EarningPeriodService.class);
        ApplicationCurrencyProvider currency = currency();
        when(rewards.findAllByStatusOrderByIdAsc(RewardStatus.PENDING)).thenReturn(pendingRewards());
        for (EarningPeriod context : EarningPeriod.values()) {
            when(periods.summarize(context)).thenReturn(available(context, "20.00"));
        }
        RewardCombinationService service = new RewardCombinationService(rewards, periods, currency);

        var relevance = service.relevantCombination();

        assertThat(relevance.evaluable()).isTrue();
        assertThat(relevance.combination()).isNull();
        verify(rewards, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private ApplicationCurrencyProvider currency() {
        ApplicationCurrencyProvider currency = mock(ApplicationCurrencyProvider.class);
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.EUR);
        return currency;
    }

    private List<Reward> pendingRewards() {
        return List.of(reward(1L, "Hamburguesas", 2, "30.00"), reward(2L, "Funkos", 2, "60.00"));
    }

    private EarningPeriodSummary available(EarningPeriod context, String amount) {
        BigDecimal value = new BigDecimal(amount);
        return new EarningPeriodSummary(context, EarningStatus.AVAILABLE, LocalDate.now(),
            LocalDate.now().plusDays(1), value, value, "EUR");
    }

    private EarningPeriodSummary unavailable(EarningPeriod context) {
        return new EarningPeriodSummary(context, EarningStatus.UNAVAILABLE, LocalDate.now(),
            LocalDate.now().plusDays(1), null, null, null);
    }

    private Reward reward(Long id, String name, int quantity, String price) {
        Reward reward = new Reward(name, quantity, new BigDecimal(price), "EUR", Instant.EPOCH);
        ReflectionTestUtils.setField(reward, "id", id);
        return reward;
    }
}
