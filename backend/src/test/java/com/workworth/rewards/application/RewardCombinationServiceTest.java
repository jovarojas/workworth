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
        when(periods.summarize(EarningPeriod.TODAY)).thenReturn(available("120.00"));
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

    private EarningPeriodSummary available(String amount) {
        BigDecimal value = new BigDecimal(amount);
        return new EarningPeriodSummary(EarningPeriod.TODAY, EarningStatus.AVAILABLE, LocalDate.now(),
            LocalDate.now().plusDays(1), value, value, "EUR");
    }

    private Reward reward(Long id, String name, int quantity, String price) {
        Reward reward = new Reward(name, quantity, new BigDecimal(price), "EUR", Instant.EPOCH);
        ReflectionTestUtils.setField(reward, "id", id);
        return reward;
    }
}
