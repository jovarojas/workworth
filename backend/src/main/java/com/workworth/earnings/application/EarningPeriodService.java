package com.workworth.earnings.application;

import com.workworth.common.money.MoneyRounding;
import com.workworth.earnings.domain.*;
import com.workworth.earnings.persistence.*;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.identity.application.CurrentUserProvider;

import java.math.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class EarningPeriodService {
    private final WorkdayEarningRepository earnings;
    private final EarningCorrectionRepository corrections;
    private final Clock clock;
    private final ApplicationCurrencyProvider applicationCurrency;
    private final CurrentUserProvider currentUser;

    public EarningPeriodService(WorkdayEarningRepository e, EarningCorrectionRepository c, Clock clock,
                                ApplicationCurrencyProvider applicationCurrency,
                                CurrentUserProvider currentUser) {
        earnings = e;
        corrections = c;
        this.clock = clock;
        this.applicationCurrency = applicationCurrency;
        this.currentUser = currentUser;
    }

    public EarningPeriodSummary summarize(EarningPeriod period) {
        ZoneId zone = ZoneId.of(currentUser.currentUser().getTimeZone());
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
        List<WorkdayEarning> periodEarnings = earnings.findAllByWorkdayOwnerId(currentUser.currentUser().getId()).stream()
            .filter(e -> start == null || (!e.getLocalDate().isBefore(start) && e.getLocalDate().isBefore(end)))
            .toList();
        List<WorkdayEarning> values = periodEarnings.stream()
            .filter(e -> e.getStatus() == EarningStatus.AVAILABLE)
            .toList();
        if (values.isEmpty() && periodEarnings.stream().anyMatch(e -> e.getStatus() == EarningStatus.UNAVAILABLE)) {
            return new EarningPeriodSummary(period, EarningStatus.UNAVAILABLE, start, end, null, null, null);
        }
        BigDecimal total = values.stream().map(this::effective).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(12, RoundingMode.HALF_UP);
        String currency = values.stream().map(WorkdayEarning::getCurrencyCode).filter(Objects::nonNull).findFirst()
            .orElse(applicationCurrency.currentCurrency().name());
        return new EarningPeriodSummary(period, EarningStatus.AVAILABLE, start, end, total,
            total.setScale(2, MoneyRounding.ROUNDING_MODE), currency);
    }

    private BigDecimal effective(WorkdayEarning e) {
        return corrections.findByEarningIdOrderBySequenceDesc(e.getId()).stream().findFirst().map(EarningCorrection::getNewAmount).orElse(e.getRawAmount());
    }
}
