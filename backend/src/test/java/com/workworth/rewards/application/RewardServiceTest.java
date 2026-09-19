package com.workworth.rewards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.workworth.rewards.exception.RewardConflictException;
import com.workworth.rewards.exception.RewardNotFoundException;
import com.workworth.rewards.persistence.Reward;
import com.workworth.rewards.persistence.RewardRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void newRewardJoinsTheEndOfTheUsersExistingPriorityOrder() {
        RewardRepository rewards = mock(RewardRepository.class);
        ApplicationCurrencyProvider currency = mock(ApplicationCurrencyProvider.class);
        CurrentUserProvider currentUser = currentUser();
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.EUR);
        when(rewards.findMaxDisplayOrderByUserId(currentUser.currentUser().getId())).thenReturn(7L);
        when(rewards.save(any(Reward.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RewardService service = new RewardService(rewards, currency, mock(ApplicationCurrencyService.class), clock(), currentUser);

        Reward reward = service.create(new CreateRewardRequest("Cine", null, new BigDecimal("12.00")));

        assertThat(reward.getDisplayOrder()).isEqualTo(8L);
    }

    @Test
    void reordersExactlyTheGivenRewardsAndLeavesTheRestUntouched() {
        RewardRepository rewards = mock(RewardRepository.class);
        CurrentUserProvider currentUser = currentUser();
        Reward first = rewardWithId(1L, currentUser.currentUser());
        Reward second = rewardWithId(2L, currentUser.currentUser());
        Reward third = rewardWithId(3L, currentUser.currentUser());
        when(rewards.findAllByIdInAndUserId(List.of(3L, 1L), currentUser.currentUser().getId()))
            .thenReturn(List.of(first, third));
        RewardService service = new RewardService(rewards, mock(ApplicationCurrencyProvider.class),
            mock(ApplicationCurrencyService.class), clock(), currentUser);

        List<Reward> reordered = service.reorder(List.of(3L, 1L));

        assertThat(reordered).extracting(Reward::getId).containsExactly(3L, 1L);
        assertThat(third.getDisplayOrder()).isZero();
        assertThat(first.getDisplayOrder()).isEqualTo(1L);
        assertThat(second.getDisplayOrder()).isEqualTo(9L);
    }

    @Test
    void rejectsReorderingARewardThatDoesNotBelongToTheCurrentUser() {
        RewardRepository rewards = mock(RewardRepository.class);
        CurrentUserProvider currentUser = currentUser();
        when(rewards.findAllByIdInAndUserId(List.of(1L, 99L), currentUser.currentUser().getId()))
            .thenReturn(List.of(rewardWithId(1L, currentUser.currentUser())));
        RewardService service = new RewardService(rewards, mock(ApplicationCurrencyProvider.class),
            mock(ApplicationCurrencyService.class), clock(), currentUser);

        assertThatThrownBy(() -> service.reorder(List.of(1L, 99L)))
            .isInstanceOf(RewardNotFoundException.class);
    }

    @Test
    void rejectsDuplicateIdsInAReorderRequest() {
        RewardRepository rewards = mock(RewardRepository.class);
        RewardService service = new RewardService(rewards, mock(ApplicationCurrencyProvider.class),
            mock(ApplicationCurrencyService.class), clock(), currentUser());

        assertThatThrownBy(() -> service.reorder(List.of(1L, 1L)))
            .isInstanceOf(RewardConflictException.class);
    }

    private Reward rewardWithId(long id, AppUser user) {
        Reward reward = new Reward(user, "Reward " + id, 1, new BigDecimal("10.00"), "EUR", Instant.EPOCH);
        reward.reorder(9L, Instant.EPOCH);
        ReflectionTestUtils.setField(reward, "id", id);
        return reward;
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
