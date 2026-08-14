package com.workworth.preferences.application;

import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.identity.persistence.AppUser;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.preferences.exception.ApplicationCurrencyLockedException;
import com.workworth.preferences.persistence.ApplicationSettings;
import com.workworth.preferences.persistence.ApplicationSettingsRepository;
import com.workworth.salary.persistence.SalaryProfileRepository;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationCurrencyService implements ApplicationCurrencyProvider {

    private final ApplicationSettingsRepository settingsRepository;
    private final SalaryProfileRepository salaryProfiles;
    private final WorkdayEarningRepository earnings;
    private final Clock clock;
    private final CurrentUserProvider currentUser;

    public ApplicationCurrencyService(ApplicationSettingsRepository settingsRepository,
                                      SalaryProfileRepository salaryProfiles,
                                      WorkdayEarningRepository earnings,
                                      Clock clock,
                                      CurrentUserProvider currentUser) {
        this.settingsRepository = settingsRepository;
        this.salaryProfiles = salaryProfiles;
        this.earnings = earnings;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    @Override
    public ApplicationCurrency currentCurrency() {
        return settings().getCurrencyCode();
    }

    public ApplicationCurrencySettings getSettings() {
        ApplicationSettings settings = settings();
        return new ApplicationCurrencySettings(settings.getCurrencyCode(), canChange(settings));
    }

    @Transactional
    public ApplicationCurrencySettings updateCurrency(ApplicationCurrency requestedCurrency) {
        ApplicationSettings settings = settings();
        if (settings.getCurrencyCode() == requestedCurrency) {
            return new ApplicationCurrencySettings(requestedCurrency, canChange(settings));
        }
        if (settings.getCurrencyLockedAt() != null || hasPersistedEconomicData()) {
            settings.lockCurrency(clock.instant());
            throw new ApplicationCurrencyLockedException(
                "The application currency cannot change after economic data has been recorded.");
        }

        settings.changeCurrency(requestedCurrency, clock.instant());
        return new ApplicationCurrencySettings(settings.getCurrencyCode(), true);
    }

    @Transactional
    public void lockCurrencyAfterEconomicData() {
        lockCurrencyAfterEconomicData(currentUser.currentUser());
    }

    @Transactional
    public void lockCurrencyAfterEconomicData(AppUser user) {
        settings(user).lockCurrency(clock.instant());
    }

    private boolean canChange(ApplicationSettings settings) {
        return settings.getCurrencyLockedAt() == null && !hasPersistedEconomicData();
    }

    private boolean hasPersistedEconomicData() {
        AppUser user = currentUser.currentUser();
        return salaryProfiles.countByUserId(user.getId()) > 0 || earnings.existsByWorkdayOwnerId(user.getId());
    }

    private ApplicationSettings settings() {
        return settings(currentUser.currentUser());
    }

    private ApplicationSettings settings(AppUser user) {
        return settingsRepository.findById(user.getId())
            .orElseGet(() -> settingsRepository.save(new ApplicationSettings(user, ApplicationCurrency.EUR, clock.instant())));
    }
}
