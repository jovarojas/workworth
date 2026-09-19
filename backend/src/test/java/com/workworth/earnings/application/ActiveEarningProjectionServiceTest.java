package com.workworth.earnings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workworth.earnings.domain.EarningProjection;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.earnings.domain.EarningUnavailableReason;
import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.identity.persistence.AppUser;
import com.workworth.salary.application.MonthlySalaryRateService;
import com.workworth.salary.domain.IncomeSource;
import com.workworth.salary.domain.MonthlySalaryRate;
import com.workworth.salary.exception.SalaryProfileNotFoundException;
import com.workworth.workday.application.WorkdayService;
import com.workworth.workday.persistence.Workday;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveEarningProjectionServiceTest {

    @Mock private WorkdayService workdays;
    @Mock private MonthlySalaryRateService rates;
    @Mock private CurrentUserProvider currentUser;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser(UUID.randomUUID(), "test|earnings", "earnings@test.invalid", "Europe/Madrid", Instant.EPOCH);
        when(currentUser.currentUser()).thenReturn(user);
    }

    // Saturday 2026-01-03 and Sunday 2026-01-04 have no WorkdaySchedule (see WorkdaySchedule.forDate).
    @Test
    void returnsNotAWorkdayOnAWeekendWithoutTryingToReconcile() {
        ActiveEarningProjectionService service = serviceAt("2026-01-03T10:00:00Z");

        EarningProjection projection = service.current();

        assertThat(projection.status()).isEqualTo(EarningStatus.UNAVAILABLE);
        assertThat(projection.unavailableReason()).isEqualTo(EarningUnavailableReason.NOT_A_WORKDAY);
        assertThat(projection.economicSeconds()).isZero();
        assertThat(projection.rawAmount()).isNull();
        assertThat(projection.currencyCode()).isNull();
        assertThat(projection.localDate()).isEqualTo(LocalDate.of(2026, 1, 3));
        verify(workdays, never()).reconcile(org.mockito.ArgumentMatchers.any(LocalDate.class));
    }

    @Test
    void returnsAnAvailableProjectionOnAWorkableWeekday() {
        // 2026-01-05 is a Monday.
        ActiveEarningProjectionService service = serviceAt("2026-01-05T10:00:00Z");
        Workday day = mock(Workday.class);
        when(workdays.reconcile(LocalDate.of(2026, 1, 5))).thenReturn(day);
        when(workdays.time(day)).thenReturn(3_600L);
        when(rates.getRate(user, YearMonth.of(2026, 1))).thenReturn(new MonthlySalaryRate(
            YearMonth.of(2026, 1), 1L, IncomeSource.NET_MONTHLY_REAL, new BigDecimal("1300.00"),
            new BigDecimal("15600.00"), 12, new BigDecimal("140.000000000000"),
            new BigDecimal("9.285714285714"), "EUR"));

        EarningProjection projection = service.current();

        assertThat(projection.status()).isEqualTo(EarningStatus.AVAILABLE);
        assertThat(projection.unavailableReason()).isNull();
        assertThat(projection.economicSeconds()).isEqualTo(3_600L);
        assertThat(projection.rawAmount()).isEqualByComparingTo("9.285714285714");
        assertThat(projection.currencyCode()).isEqualTo("EUR");
    }

    @Test
    void keepsExistingSalaryUnavailableReasonsOnAWorkableWeekday() {
        ActiveEarningProjectionService service = serviceAt("2026-01-05T10:00:00Z");
        Workday day = mock(Workday.class);
        when(workdays.reconcile(LocalDate.of(2026, 1, 5))).thenReturn(day);
        when(workdays.time(day)).thenReturn(1_800L);
        when(rates.getRate(user, YearMonth.of(2026, 1)))
            .thenThrow(new SalaryProfileNotFoundException("no profile"));

        EarningProjection projection = service.current();

        assertThat(projection.status()).isEqualTo(EarningStatus.UNAVAILABLE);
        assertThat(projection.unavailableReason()).isEqualTo(EarningUnavailableReason.SALARY_PROFILE_NOT_FOUND);
        assertThat(projection.economicSeconds()).isEqualTo(1_800L);
    }

    private ActiveEarningProjectionService serviceAt(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"));
        return new ActiveEarningProjectionService(workdays, rates, clock, currentUser);
    }
}
