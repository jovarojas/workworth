package com.workworth.salary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.workworth.salary.api.dto.CreateSalaryProfileRequest;
import com.workworth.salary.api.dto.SalaryProfileResponse;
import com.workworth.salary.exception.SalaryProfileConflictException;
import com.workworth.salary.persistence.SalaryProfile;
import com.workworth.salary.persistence.SalaryProfileRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

    private SalaryProfileService salaryProfileService;

    @BeforeEach
    void setUp() {
        salaryProfileService = new SalaryProfileService(
                salaryProfileRepository, new SalaryProfileMapper(), clock);
    }

    @Test
    void createsRealNetProfileAndDerivesAnnualNet() {
        CreateSalaryProfileRequest request = request(LocalDate.of(2026, 8, 1), new BigDecimal("1250.00"));
        when(salaryProfileRepository.existsByEffectiveFrom(request.effectiveFrom())).thenReturn(false);
        when(salaryProfileRepository.save(any(SalaryProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalaryProfileResponse response = salaryProfileService.create(request);

        assertThat(response.netAnnualReal()).isEqualByComparingTo("15000.00");
        assertThat(response.activeIncomeSource().name()).isEqualTo("NET_MONTHLY_REAL");
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

    private CreateSalaryProfileRequest request(LocalDate effectiveFrom, BigDecimal netMonthlyReal) {
        return new CreateSalaryProfileRequest(
                effectiveFrom,
                new BigDecimal("19000.00"),
                netMonthlyReal,
                "EUR",
                12);
    }
}
