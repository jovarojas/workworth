package com.workworth.earnings.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.salary.application.MonthlySalaryRateService;
import com.workworth.salary.exception.SalaryRateUnavailableException;
import com.workworth.workday.application.WorkdayService;
import com.workworth.workday.domain.WorkdayStatus;
import com.workworth.workday.persistence.Workday;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class EarningMaterializationServiceTest {

    @Test
    void rejectsMaterializationBeforeTheWorkdayIsCompleted() {
        WorkdayEarningRepository earnings = mock(WorkdayEarningRepository.class);
        Workday day = mock(Workday.class);
        when(day.getStatus()).thenReturn(WorkdayStatus.ACTIVE);

        EarningMaterializationService service = new EarningMaterializationService(
                earnings, mock(WorkdayService.class), mock(MonthlySalaryRateService.class),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Europe/Madrid")));

        assertThatThrownBy(() -> service.materialize(day)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(earnings);
    }

    @Test
    void recordsAnExplicitUnavailableReasonWhenNoMonthlyRateExists() {
        WorkdayEarningRepository earnings = mock(WorkdayEarningRepository.class);
        WorkdayService workdays = mock(WorkdayService.class);
        MonthlySalaryRateService rates = mock(MonthlySalaryRateService.class);
        Workday day = mock(Workday.class);
        when(day.getStatus()).thenReturn(WorkdayStatus.COMPLETED);
        when(day.getId()).thenReturn(4L);
        when(day.getLocalDate()).thenReturn(java.time.LocalDate.of(2026, 1, 2));
        when(workdays.time(day)).thenReturn(28_800L);
        when(rates.getRate(java.time.YearMonth.of(2026, 1)))
                .thenThrow(new SalaryRateUnavailableException("No calendar."));
        when(earnings.findByWorkdayId(4L)).thenReturn(java.util.Optional.empty());
        when(earnings.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = new EarningMaterializationService(earnings, workdays, rates,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Europe/Madrid")))
                .materialize(day);

        org.assertj.core.api.Assertions.assertThat(saved.getStatus())
                .isEqualTo(com.workworth.earnings.domain.EarningStatus.UNAVAILABLE);
        org.assertj.core.api.Assertions.assertThat(saved.getUnavailableReason())
                .isEqualTo(com.workworth.earnings.domain.EarningUnavailableReason.SALARY_RATE_UNAVAILABLE);
        verify(earnings).save(saved);
    }
}
