package com.workworth.preferences.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.preferences.exception.ApplicationCurrencyLockedException;
import com.workworth.preferences.persistence.ApplicationSettings;
import com.workworth.preferences.persistence.ApplicationSettingsRepository;
import com.workworth.salary.persistence.SalaryProfileRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationCurrencyServiceTest {

    private final ApplicationSettings settings = new ApplicationSettings(
        ApplicationCurrency.EUR, Instant.parse("2026-08-01T00:00:00Z"));
    private final ApplicationSettingsRepository settingsRepository = mock(ApplicationSettingsRepository.class);
    private final SalaryProfileRepository salaryProfiles = mock(SalaryProfileRepository.class);
    private final WorkdayEarningRepository earnings = mock(WorkdayEarningRepository.class);
    private ApplicationCurrencyService service;

    @BeforeEach
    void setUp() {
        when(settingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
        when(salaryProfiles.count()).thenReturn(0L);
        when(earnings.count()).thenReturn(0L);
        service = new ApplicationCurrencyService(settingsRepository, salaryProfiles, earnings,
            Clock.fixed(Instant.parse("2026-08-12T10:00:00Z"), ZoneOffset.UTC));
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
        when(salaryProfiles.count()).thenReturn(1L);

        assertThatThrownBy(() -> service.updateCurrency(ApplicationCurrency.USD))
            .isInstanceOf(ApplicationCurrencyLockedException.class);

        assertThat(settings.getCurrencyCode()).isEqualTo(ApplicationCurrency.EUR);
        assertThat(settings.getCurrencyLockedAt()).isEqualTo(Instant.parse("2026-08-12T10:00:00Z"));
    }

    @Test
    void locksCurrencyWhenAMaterializedEarningAlreadyExists() {
        when(earnings.count()).thenReturn(1L);

        assertThatThrownBy(() -> service.updateCurrency(ApplicationCurrency.USD))
            .isInstanceOf(ApplicationCurrencyLockedException.class);

        assertThat(settings.getCurrencyLockedAt()).isEqualTo(Instant.parse("2026-08-12T10:00:00Z"));
    }
}
