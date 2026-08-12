package com.workworth.earnings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workworth.earnings.domain.EarningCorrectionCause;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.earnings.domain.EarningUnavailableReason;
import com.workworth.earnings.api.dto.EarningResponse;
import com.workworth.earnings.persistence.EarningCorrection;
import com.workworth.earnings.persistence.EarningCorrectionRepository;
import com.workworth.earnings.persistence.WorkdayEarning;
import com.workworth.earnings.persistence.WorkdayEarningRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EarningQueryServiceTest {

    @Mock private WorkdayEarningRepository earnings;
    @Mock private EarningCorrectionRepository corrections;

    private EarningQueryService service;

    @BeforeEach
    void setUp() {
        service = new EarningQueryService(earnings, corrections);
    }

    @Test
    void returnsAnEmptyHistoryPageWithoutLoadingCorrections() {
        when(earnings.findAllByOrderByLocalDateDesc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));

        EarningHistoryPage result = service.history(0, 20);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        verify(corrections, never()).findByEarningIdInOrderByEarningIdAscSequenceDesc(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsBaseValuesWhenAnEarningHasNoCorrection() {
        WorkdayEarning earning = earning(1L, "2026-07-06", EarningStatus.AVAILABLE, 3_600L, "12.345000000000");
        when(earnings.findAllByOrderByLocalDateDesc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(earning), org.springframework.data.domain.PageRequest.of(0, 1), 1));
        when(corrections.findByEarningIdInOrderByEarningIdAscSequenceDesc(List.of(1L))).thenReturn(List.of());

        EffectiveEarning result = service.history(0, 1).items().get(0);

        assertThat(result.economicSeconds()).isEqualTo(3_600L);
        assertThat(result.amount()).isEqualByComparingTo("12.345000000000");
    }

    @Test
    void usesOnlyTheHighestSequenceCorrectionForTheEffectiveValues() {
        WorkdayEarning earning = earning(1L, "2026-07-06", EarningStatus.AVAILABLE, 3_600L, "12.000000000000");
        EarningCorrection newest = correction(earning, 3, 1_200L, "4.000000000000");
        EarningCorrection older = correction(earning, 2, 1_800L, "6.000000000000");
        when(earnings.findAllByOrderByLocalDateDesc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(earning), org.springframework.data.domain.PageRequest.of(0, 1), 1));
        when(corrections.findByEarningIdInOrderByEarningIdAscSequenceDesc(List.of(1L)))
                .thenReturn(List.of(newest, older));

        EffectiveEarning result = service.history(0, 1).items().get(0);

        assertThat(result.economicSeconds()).isEqualTo(1_200L);
        assertThat(result.amount()).isEqualByComparingTo("4.000000000000");
        verify(corrections, never()).findByEarningIdOrderBySequenceDesc(1L);
    }

    @Test
    void resolvesAllPageCorrectionsWithOneBulkQueryAndPreservesPageMetadata() {
        WorkdayEarning newest = earning(3L, "2026-07-08", EarningStatus.AVAILABLE, 3_600L, "12.000000000000");
        WorkdayEarning middle = earning(2L, "2026-07-07", EarningStatus.AVAILABLE, 3_600L, "12.000000000000");
        WorkdayEarning oldest = earning(1L, "2026-07-06", EarningStatus.AVAILABLE, 3_600L, "12.000000000000");
        when(earnings.findAllByOrderByLocalDateDesc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(middle), org.springframework.data.domain.PageRequest.of(1, 1), 3));
        when(corrections.findByEarningIdInOrderByEarningIdAscSequenceDesc(List.of(2L)))
                .thenReturn(List.of(correction(middle, 1, 1_800L, "6.000000000000")));

        EarningHistoryPage result = service.history(1, 1);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.items()).extracting(item -> item.base().getLocalDate())
                .containsExactly(LocalDate.of(2026, 7, 7));
        verify(corrections).findByEarningIdInOrderByEarningIdAscSequenceDesc(List.of(2L));
        verify(corrections, never()).findByEarningIdOrderBySequenceDesc(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void keepsAnUnavailableEarningUnavailableWithoutAnInventedAmount() {
        WorkdayEarning unavailable = new WorkdayEarning(1L, LocalDate.of(2026, 7, 6), EarningStatus.UNAVAILABLE,
                EarningUnavailableReason.SALARY_RATE_UNAVAILABLE, 3_600L, null, null, null, null, null, 0,
                null, null, null, Instant.parse("2026-07-01T00:00:00Z"));
        ReflectionTestUtils.setField(unavailable, "id", 1L);
        when(earnings.findAllByOrderByLocalDateDesc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(unavailable), org.springframework.data.domain.PageRequest.of(0, 1), 1));
        when(corrections.findByEarningIdInOrderByEarningIdAscSequenceDesc(List.of(1L))).thenReturn(List.of());

        EffectiveEarning result = service.history(0, 1).items().get(0);

        assertThat(result.amount()).isNull();
        assertThat(result.economicSeconds()).isEqualTo(3_600L);
        assertThat(EarningResponse.from(result).unavailableReason()).isEqualTo("SALARY_RATE_UNAVAILABLE");
    }

    @Test
    void usesTheSameEffectiveSemanticsForTheSingleWorkdayQuery() {
        WorkdayEarning earning = earning(1L, "2026-07-06", EarningStatus.AVAILABLE, 3_600L, "12.000000000000");
        when(earnings.findByLocalDate(LocalDate.of(2026, 7, 6))).thenReturn(Optional.of(earning));
        when(corrections.findByEarningIdOrderBySequenceDesc(1L))
                .thenReturn(List.of(correction(earning, 2, 900L, "3.000000000000"), correction(earning, 1, 1_800L, "6.000000000000")));

        EffectiveEarning result = service.byDate(LocalDate.of(2026, 7, 6));

        assertThat(result.economicSeconds()).isEqualTo(900L);
        assertThat(result.amount()).isEqualByComparingTo("3.000000000000");
    }

    private WorkdayEarning earning(Long id, String localDate, EarningStatus status, long seconds, String amount) {
        WorkdayEarning earning = new WorkdayEarning(id, LocalDate.parse(localDate), status, seconds,
                amount == null ? null : new BigDecimal(amount), 1L, null, new BigDecimal("1300.00"),
                new BigDecimal("15600.00"), 12, "EUR", new BigDecimal("140.000000000000"),
                new BigDecimal("9.285714285714"), Instant.parse("2026-07-01T00:00:00Z"));
        ReflectionTestUtils.setField(earning, "id", id);
        return earning;
    }

    private EarningCorrection correction(WorkdayEarning earning, int sequence, long seconds, String amount) {
        return new EarningCorrection(earning, (long) sequence, null, sequence,
                EarningCorrectionCause.PARTIAL_ABSENCE_CHANGED, earning.getEconomicSeconds(), seconds,
                earning.getRawAmount(), new BigDecimal(amount), Instant.parse("2026-07-02T00:00:00Z"));
    }
}
