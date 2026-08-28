package com.workworth.earnings.application;

import com.workworth.workday.domain.WorkdayCompletedEvent;
import com.workworth.workday.persistence.WorkdayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkdayCompletionEarningListener {
    private static final Logger log = LoggerFactory.getLogger(WorkdayCompletionEarningListener.class);

    private final WorkdayRepository workdays;
    private final EarningMaterializationService earnings;

    public WorkdayCompletionEarningListener(WorkdayRepository workdays, EarningMaterializationService earnings) {
        this.workdays = workdays;
        this.earnings = earnings;
    }

    // AFTER_COMMIT (not BEFORE_COMMIT) + its own REQUIRES_NEW transaction: a workday is its own
    // reconciliation unit. By the time this runs, the Workday this event belongs to -- and every
    // other workday reconciled in the same reconcileThroughToday() call -- has already committed
    // independently. Whatever goes wrong materializing THIS workday's Earning (a missing salary
    // profile, or anything else) is contained to this workday's own small transaction and can
    // never roll back a sibling date's already-persisted Workday or Earning.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void materialize(WorkdayCompletedEvent event) {
        try {
            workdays.findById(event.workdayId()).ifPresent(earnings::materialize);
        } catch (RuntimeException e) {
            log.warn("Failed to materialize earning for workday {}", event.workdayId(), e);
        }
    }
}
