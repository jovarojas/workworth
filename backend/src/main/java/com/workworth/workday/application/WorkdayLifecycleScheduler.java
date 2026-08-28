package com.workworth.workday.application;

import com.workworth.identity.domain.AppUserStatus;
import com.workworth.identity.persistence.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkdayLifecycleScheduler {
    private static final Logger log = LoggerFactory.getLogger(WorkdayLifecycleScheduler.class);

    private final WorkdayService service;
    private final AppUserRepository users;

    public WorkdayLifecycleScheduler(WorkdayService service, AppUserRepository users) {
        this.service = service;
        this.users = users;
    }

    @Scheduled(fixedDelay = 60000)
    public void reconcileToday() {
        // Isolated per user: one user's failure must not stop the rest of the batch from being
        // reconciled on this tick, and must leave a trace instead of vanishing silently.
        users.findAllByStatus(AppUserStatus.ACTIVE).forEach(user -> {
            try {
                service.reconcileThroughToday(user);
            } catch (RuntimeException e) {
                log.warn("Failed to reconcile workdays for user {}", user.getId(), e);
            }
        });
    }
}
