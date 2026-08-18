package com.workworth.goals.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workworth.goals.api.dto.CreateGoalRequest;
import com.workworth.goals.api.dto.UpdateGoalRequest;
import com.workworth.goals.domain.GoalStatus;
import com.workworth.goals.exception.GoalConflictException;
import com.workworth.goals.exception.GoalProgressUnavailableException;
import com.workworth.goals.persistence.Goal;
import com.workworth.goals.persistence.GoalRepository;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.application.ApplicationCurrencyService;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.identity.application.TestUsers;
import com.workworth.identity.persistence.AppUser;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class GoalServiceTest {

    @Test
    void createsAnActiveGoalWithTheGlobalCurrencyAndLocksIt() {
        GoalRepository goals = mock(GoalRepository.class);
        GoalProgressService progress = mock(GoalProgressService.class);
        ApplicationCurrencyProvider currency = mock(ApplicationCurrencyProvider.class);
        ApplicationCurrencyService currencyService = mock(ApplicationCurrencyService.class);
        when(currency.currentCurrency()).thenReturn(ApplicationCurrency.USD);
        when(goals.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GoalService service = service(goals, progress, currency, currencyService);

        Goal goal = service.create(new CreateGoalRequest("Viaje", new BigDecimal("500.00")));

        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(goal.getCurrencyCode()).isEqualTo("USD");
        verify(currencyService).lockCurrencyAfterEconomicData();
    }

    @Test
    void completesOnlyAReachedActiveGoalAndKeepsClosedGoalsTerminal() {
        GoalRepository goals = mock(GoalRepository.class);
        CurrentUserProvider currentUser = currentUser();
        Goal goal = new Goal(currentUser.currentUser(), "Viaje", new BigDecimal("100.00"), "EUR", Instant.EPOCH);
        when(goals.findByIdAndUserIdForUpdate(7L, currentUser.currentUser().getId())).thenReturn(Optional.of(goal));
        GoalProgressService progress = mock(GoalProgressService.class);
        when(progress.resolve(goal)).thenReturn(new GoalProgress(true, new BigDecimal("100.00"), BigDecimal.ZERO,
            new BigDecimal("100.00"), true));
        GoalService service = service(goals, progress, mock(ApplicationCurrencyProvider.class),
            mock(ApplicationCurrencyService.class), currentUser);

        Goal completed = service.complete(7L);

        assertThat(completed.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(completed.getClosedAt()).isNotNull();
        assertThatThrownBy(() -> service.update(7L, new UpdateGoalRequest("Otro", new BigDecimal("50.00"))))
            .isInstanceOf(GoalConflictException.class);
        assertThatThrownBy(() -> service.cancel(7L)).isInstanceOf(GoalConflictException.class);
    }

    @Test
    void rejectsCompletionWhenProgressIsUnavailableOrNotReached() {
        GoalRepository goals = mock(GoalRepository.class);
        CurrentUserProvider currentUser = currentUser();
        Goal unavailableGoal = new Goal(currentUser.currentUser(), "Viaje", new BigDecimal("100.00"), "EUR", Instant.EPOCH);
        when(goals.findByIdAndUserIdForUpdate(1L, currentUser.currentUser().getId())).thenReturn(Optional.of(unavailableGoal));
        GoalProgressService progress = mock(GoalProgressService.class);
        when(progress.resolve(unavailableGoal)).thenReturn(GoalProgress.unavailable());
        GoalService service = service(goals, progress, mock(ApplicationCurrencyProvider.class),
            mock(ApplicationCurrencyService.class), currentUser);

        assertThatThrownBy(() -> service.complete(1L)).isInstanceOf(GoalProgressUnavailableException.class);

        Goal unreachedGoal = new Goal(currentUser.currentUser(), "Curso", new BigDecimal("100.00"), "EUR", Instant.EPOCH);
        when(goals.findByIdAndUserIdForUpdate(2L, currentUser.currentUser().getId())).thenReturn(Optional.of(unreachedGoal));
        when(progress.resolve(unreachedGoal)).thenReturn(new GoalProgress(true, new BigDecimal("50.00"),
            new BigDecimal("50.00"), new BigDecimal("50.00"), false));

        assertThatThrownBy(() -> service.complete(2L)).isInstanceOf(GoalConflictException.class);
    }

    @Test
    void editsAndCancelsOnlyAnActiveGoal() {
        GoalRepository goals = mock(GoalRepository.class);
        CurrentUserProvider currentUser = currentUser();
        Goal goal = new Goal(currentUser.currentUser(), "Viaje", new BigDecimal("100.00"), "EUR", Instant.EPOCH);
        when(goals.findByIdAndUserIdForUpdate(3L, currentUser.currentUser().getId())).thenReturn(Optional.of(goal));
        GoalService service = service(goals, mock(GoalProgressService.class), mock(ApplicationCurrencyProvider.class),
            mock(ApplicationCurrencyService.class), currentUser);

        Goal updated = service.update(3L, new UpdateGoalRequest("Viaje largo", new BigDecimal("150.00")));
        Goal cancelled = service.cancel(3L);

        assertThat(updated.getTitle()).isEqualTo("Viaje largo");
        assertThat(updated.getTargetAmount()).isEqualByComparingTo("150.00");
        assertThat(cancelled.getStatus()).isEqualTo(GoalStatus.CANCELLED);
        assertThat(cancelled.getClosedAt()).isNotNull();
    }

    private GoalService service(GoalRepository goals, GoalProgressService progress, ApplicationCurrencyProvider currency,
                                ApplicationCurrencyService currencyService) {
        return service(goals, progress, currency, currencyService, currentUser());
    }

    private GoalService service(GoalRepository goals, GoalProgressService progress, ApplicationCurrencyProvider currency,
                                ApplicationCurrencyService currencyService, CurrentUserProvider currentUser) {
        return new GoalService(goals, progress, currency, currencyService,
            Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneOffset.UTC), currentUser);
    }

    private CurrentUserProvider currentUser() {
        AppUser user = new AppUser(UUID.randomUUID(), "test|goal", "goal@test.invalid", "Europe/Madrid", Instant.EPOCH);
        CurrentUserProvider provider = mock(CurrentUserProvider.class);
        when(provider.currentUser()).thenReturn(user);
        return provider;
    }
}
