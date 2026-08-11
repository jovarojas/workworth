package com.workworth.earnings.application;

import com.workworth.earnings.domain.*;
import com.workworth.earnings.persistence.*;
import com.workworth.workday.domain.WorkdayTimeCorrectionCause;
import com.workworth.workday.domain.WorkdayTimeCorrectionRegisteredEvent;
import com.workworth.workday.persistence.WorkdayTimeCorrection;
import com.workworth.workday.persistence.WorkdayTimeCorrectionRepository;

import java.math.*;
import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class EarningCorrectionService {
    private final WorkdayEarningRepository earnings;
    private final EarningCorrectionRepository corrections;
    private final WorkdayTimeCorrectionRepository workdayCorrections;
    private final Clock clock;

    public EarningCorrectionService(WorkdayEarningRepository e, EarningCorrectionRepository c, WorkdayTimeCorrectionRepository w, Clock clock) {
        earnings = e;
        corrections = c;
        workdayCorrections = w;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void consume(WorkdayTimeCorrectionRegisteredEvent event) {
        workdayCorrections.findById(event.workdayTimeCorrectionId()).ifPresent(this::apply);
    }

    @Transactional
    public void apply(WorkdayTimeCorrection source) {
        if (corrections.existsByWorkdayTimeCorrectionId(source.getId())) return;
        WorkdayEarning base = earnings.findByWorkdayId(source.getWorkday().getId()).orElse(null);
        if (base == null || base.getStatus() == EarningStatus.UNAVAILABLE) return;
        var previous = corrections.findByEarningIdOrderBySequenceDesc(base.getId()).stream().findFirst();
        long old = previous.map(EarningCorrection::getNewEconomicSeconds).orElse(base.getEconomicSeconds());
        BigDecimal oldAmount = previous.map(EarningCorrection::getNewAmount).orElse(base.getRawAmount());
        BigDecimal next = amount(base, source.getNewEconomicSeconds());
        corrections.save(new EarningCorrection(base, source.getId(), previous.orElse(null), previous.map(x -> x.getSequence() + 1).orElse(1), EarningCorrectionCause.valueOf(source.getCause().name()), old, source.getNewEconomicSeconds(), oldAmount, next, clock.instant()));
    }

    private BigDecimal amount(WorkdayEarning e, long seconds) {
        return e.getHourlyRate().multiply(BigDecimal.valueOf(seconds)).divide(BigDecimal.valueOf(3600), 12, RoundingMode.HALF_UP);
    }
}
