package com.workworth.earnings.application;

import com.workworth.workday.domain.WorkdayCompletedEvent;
import com.workworth.workday.persistence.WorkdayRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkdayCompletionEarningListener {
    private final WorkdayRepository workdays;
    private final EarningMaterializationService earnings;

    public WorkdayCompletionEarningListener(WorkdayRepository workdays, EarningMaterializationService earnings) {
        this.workdays = workdays;
        this.earnings = earnings;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void materialize(WorkdayCompletedEvent event) {
        workdays.findById(event.workdayId()).ifPresent(earnings::materialize);
    }
}
