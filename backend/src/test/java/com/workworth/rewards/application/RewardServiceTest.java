package com.workworth.rewards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.application.ApplicationCurrencyService;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.identity.application.TestUsers;
import com.workworth.identity.persistence.AppUser;
import com.workworth.rewards.api.dto.CreateRewardRequest;
import com.workworth.rewards.domain.RewardStatus;
import com.workworth.rewards.persistence.Reward;
import com.workworth.rewards.persistence.RewardRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RewardServiceTest {

    @Test
    void createsPendingRewardWithDefaultQuantityAndTotalPriceWithoutMultiplyingIt() {
        RewardRepository rewards = mock(RewardRepository.class);
        ApplicationCurrencyProvider currency = mock(ApplicationCurrencyProvider.class);
        ApplicationCurrencyService currencyService = mock(ApplicationCurrencyService.class);
        CurrentUserProvider currentUser = currentUser();
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.EUR);
        when(rewards.save(any(Reward.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RewardService service = new RewardService(rewards, currency, currencyService, clock(), currentUser);

        Reward reward = service.create(new CreateRewardRequest("Hamburguesas", null, new BigDecimal("30.00")));

        assertThat(reward.getQuantity()).isEqualTo(1);
        assertThat(reward.getPrice()).isEqualByComparingTo("30.00");
        assertThat(reward.getCurrencyCode()).isEqualTo("EUR");
        assertThat(reward.getStatus()).isEqualTo(RewardStatus.PENDING);
        verify(currencyService).lockCurrencyAfterEconomicData();
    }

    @Test
    void acquiresRewardIdempotently() {
        RewardRepository rewards = mock(RewardRepository.class);
        CurrentUserProvider currentUser = currentUser();
        Reward reward = new Reward(currentUser.currentUser(), "Libro", 1, new BigDecimal("20.00"), "EUR", Instant.EPOCH);
        when(rewards.findByIdAndUserIdForUpdate(8L, currentUser.currentUser().getId())).thenReturn(Optional.of(reward));
        RewardService service = new RewardService(rewards, mock(ApplicationCurrencyProvider.class),
            mock(ApplicationCurrencyService.class), clock(), currentUser);

        service.acquire(8L);
        service.acquire(8L);

        assertThat(reward.getStatus()).isEqualTo(RewardStatus.ACQUIRED);
    }

    private Clock clock() {
        return Clock.fixed(Instant.parse("2026-08-12T10:00:00Z"), ZoneOffset.UTC);
    }

    private CurrentUserProvider currentUser() {
        AppUser user = new AppUser(UUID.randomUUID(), "test|reward", "reward@test.invalid", "Europe/Madrid", Instant.EPOCH);
        CurrentUserProvider provider = mock(CurrentUserProvider.class);
        when(provider.currentUser()).thenReturn(user);
        return provider;
    }
}
