package com.workworth.earnings.application;

import com.workworth.common.money.MoneyRounding;
import com.workworth.earnings.domain.*;
import com.workworth.earnings.persistence.*;

import java.math.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EarningPeriodService {
    private final WorkdayEarningRepository earnings;
    private final EarningCorrectionRepository corrections;
    private final Clock clock;
    private final ZoneId zone;

    public EarningPeriodService(WorkdayEarningRepository e, EarningCorrectionRepository c, Clock clock, @Value("${workworth.time-zone:Europe/Madrid}") String zone) {
        earnings = e;
        corrections = c;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    public EarningPeriodSummary summarize(EarningPeriod period) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate start = switch (period) {
            case TODAY -> today;
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> today.withDayOfMonth(1);
            case ALL_TIME -> null;
        };
        LocalDate end = switch (period) {
            case TODAY -> today.plusDays(1);
            case WEEK -> start.plusDays(7);
            case MONTH -> start.plusMonths(1);
            case ALL_TIME -> null;
        };
        List<WorkdayEarning> periodEarnings = earnings.findAll().stream()
            .filter(e -> start == null || (!e.getLocalDate().isBefore(start) && e.getLocalDate().isBefore(end)))
            .toList();
        List<WorkdayEarning> values = periodEarnings.stream()
            .filter(e -> e.getStatus() == EarningStatus.AVAILABLE)
            .toList();
        if (values.isEmpty() && periodEarnings.stream().anyMatch(e -> e.getStatus() == EarningStatus.UNAVAILABLE)) {
            return new EarningPeriodSummary(period, EarningStatus.UNAVAILABLE, start, end, null, null, null);
        }
        BigDecimal total = values.stream().map(this::effective).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(12, RoundingMode.HALF_UP);
        String currency = values.stream().map(WorkdayEarning::getCurrencyCode).filter(Objects::nonNull).findFirst().orElse("EUR");
        return new EarningPeriodSummary(period, EarningStatus.AVAILABLE, start, end, total,
            total.setScale(2, MoneyRounding.ROUNDING_MODE), currency);
    }

    private BigDecimal effective(WorkdayEarning e) {
        return corrections.findByEarningIdOrderBySequenceDesc(e.getId()).stream().findFirst().map(EarningCorrection::getNewAmount).orElse(e.getRawAmount());
    }
}
