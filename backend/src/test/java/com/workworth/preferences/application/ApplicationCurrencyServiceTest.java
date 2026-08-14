package com.workworth.preferences.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.identity.persistence.AppUser;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.preferences.exception.ApplicationCurrencyLockedException;
import com.workworth.preferences.persistence.ApplicationSettings;
import com.workworth.preferences.persistence.ApplicationSettingsRepository;
import com.workworth.salary.persistence.SalaryProfileRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationCurrencyServiceTest {

    private final AppUser user = new AppUser(UUID.randomUUID(), "test|preferences", "preferences@test.invalid",
        "Europe/Madrid", Instant.EPOCH);
    private final ApplicationSettings settings = new ApplicationSettings(user,
        ApplicationCurrency.EUR, Instant.parse("2026-08-01T00:00:00Z"));
    private final ApplicationSettingsRepository settingsRepository = mock(ApplicationSettingsRepository.class);
    private final SalaryProfileRepository salaryProfiles = mock(SalaryProfileRepository.class);
    private final WorkdayEarningRepository earnings = mock(WorkdayEarningRepository.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private ApplicationCurrencyService service;

    @BeforeEach
    void setUp() {
        when(currentUser.currentUser()).thenReturn(user);
        when(settingsRepository.findById(user.getId())).thenReturn(Optional.of(settings));
        when(salaryProfiles.countByUserId(user.getId())).thenReturn(0L);
        when(earnings.existsByWorkdayOwnerId(user.getId())).thenReturn(false);
        service = new ApplicationCurrencyService(settingsRepository, salaryProfiles, earnings,
            Clock.fixed(Instant.parse("2026-08-12T10:00:00Z"), ZoneOffset.UTC), currentUser);
    }

    @Test
    void changesCurrencyBeforeEconomicDataExists() {
        var response = service.updateCurrency(ApplicationCurrency.USD);

        assertThat(response.currencyCode()).isEqualTo(ApplicationCurrency.USD);
        assertThat(response.changeAllowed()).isTrue();
        assertThat(service.currentCurrency()).isEqualTo(ApplicationCurrency.USD);
    }

    @Test
    void locksCurrencyWhenASalaryProfileAlreadyExists() {
        when(salaryProfiles.countByUserId(user.getId())).thenReturn(1L);

        assertThatThrownBy(() -> service.updateCurrency(ApplicationCurrency.USD))
            .isInstanceOf(ApplicationCurrencyLockedException.class);

        assertThat(settings.getCurrencyCode()).isEqualTo(ApplicationCurrency.EUR);
        assertThat(settings.getCurrencyLockedAt()).isEqualTo(Instant.parse("2026-08-12T10:00:00Z"));
    }

    @Test
    void locksCurrencyWhenAMaterializedEarningAlreadyExists() {
        when(earnings.existsByWorkdayOwnerId(user.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.updateCurrency(ApplicationCurrency.USD))
            .isInstanceOf(ApplicationCurrencyLockedException.class);

        assertThat(settings.getCurrencyLockedAt()).isEqualTo(Instant.parse("2026-08-12T10:00:00Z"));
    }
}
