package com.workworth.salary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.application.ApplicationCurrencyService;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.salary.api.dto.CreateSalaryProfileRequest;
import com.workworth.salary.api.dto.SalaryProfileResponse;
import com.workworth.salary.api.dto.UpcomingSalaryProfileResponse;
import com.workworth.salary.exception.SalaryProfileConflictException;
import com.workworth.salary.persistence.SalaryProfile;
import com.workworth.salary.persistence.SalaryProfileRepository;
import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.identity.persistence.AppUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class SalaryProfileServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-10T08:00:00Z"), ZoneId.of("Europe/Madrid"));

    @Mock
    private SalaryProfileRepository salaryProfileRepository;

    @Mock
    private ApplicationCurrencyProvider applicationCurrency;

    @Mock
    private ApplicationCurrencyService applicationCurrencyService;

    @Mock
    private CurrentUserProvider currentUser;

    private SalaryProfileService salaryProfileService;

    @BeforeEach
    void setUp() {
        salaryProfileService = new SalaryProfileService(
                salaryProfileRepository, new SalaryProfileMapper(), clock,
                applicationCurrency, applicationCurrencyService, currentUser);
        when(applicationCurrency.currentCurrency()).thenReturn(ApplicationCurrency.EUR);
        org.mockito.Mockito.lenient().when(currentUser.currentUser()).thenReturn(new AppUser(UUID.randomUUID(), "test|salary",
            "salary@test.invalid", "Europe/Madrid", Instant.EPOCH));
    }

    @Test
    void createsRealNetProfileAndDerivesAnnualNet() {
        CreateSalaryProfileRequest request = request(LocalDate.of(2026, 8, 1), new BigDecimal("1250.00"));
        when(salaryProfileRepository.existsByUserIdAndEffectiveFrom(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(request.effectiveFrom()))).thenReturn(false);
        when(salaryProfileRepository.save(any(SalaryProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalaryProfileResponse response = salaryProfileService.create(request);

        assertThat(response.netAnnualReal()).isEqualByComparingTo("15000.00");
        assertThat(response.activeIncomeSource().name()).isEqualTo("NET_MONTHLY_REAL");
        verify(applicationCurrencyService).lockCurrencyAfterEconomicData();
    }

    @Test
    void rejectsSalaryChangesBeforeCurrentMonth() {
        CreateSalaryProfileRequest request = request(LocalDate.of(2026, 7, 1), new BigDecimal("1250.00"));

        assertThatThrownBy(() -> salaryProfileService.create(request))
                .isInstanceOf(SalaryProfileConflictException.class)
                .hasMessageContaining("before the current month");
    }

    @Test
    void rejectsEffectiveDatesThatAreNotTheFirstDayOfMonth() {
        CreateSalaryProfileRequest request = request(LocalDate.of(2026, 9, 2), new BigDecimal("1250.00"));

        assertThatThrownBy(() -> salaryProfileService.create(request))
                .isInstanceOf(SalaryProfileConflictException.class)
                .hasMessageContaining("first day");
    }

    @Test
    void getUpcomingReturnsNullWhenNoFutureSalaryBasisIsScheduled() {
        // The fixture clock sits at 2026-08-10, so "current month" is August 2026 and the
        // boundary the service asks the repository about is 2026-08-01.
        when(salaryProfileRepository.findFirstByUserIdAndEffectiveFromGreaterThanOrderByEffectiveFromAsc(
                any(), org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 8, 1))))
            .thenReturn(java.util.Optional.empty());

        UpcomingSalaryProfileResponse upcoming = salaryProfileService.getUpcoming();

        assertThat(upcoming.salaryProfile()).isNull();
    }

    @Test
    void getUpcomingReturnsAnAlreadyScheduledFutureSalaryBasis() {
        AppUser user = currentUser.currentUser();
        SalaryProfile scheduled = new SalaryProfile(user, LocalDate.of(2026, 9, 1), null,
            new BigDecimal("1500.00"), "EUR", 12, Instant.EPOCH);
        when(salaryProfileRepository.findFirstByUserIdAndEffectiveFromGreaterThanOrderByEffectiveFromAsc(
                any(), org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 8, 1))))
            .thenReturn(java.util.Optional.of(scheduled));

        UpcomingSalaryProfileResponse upcoming = salaryProfileService.getUpcoming();

        assertThat(upcoming.salaryProfile()).isNotNull();
        assertThat(upcoming.salaryProfile().effectiveFrom()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(upcoming.salaryProfile().netMonthlyReal()).isEqualByComparingTo("1500.00");
    }

    private CreateSalaryProfileRequest request(LocalDate effectiveFrom, BigDecimal netMonthlyReal) {
        return new CreateSalaryProfileRequest(
                effectiveFrom,
                new BigDecimal("19000.00"),
                netMonthlyReal,
                "EUR",
                12);
    }
}
