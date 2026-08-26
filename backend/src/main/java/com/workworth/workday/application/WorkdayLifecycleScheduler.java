package com.workworth.workday.application;

import com.workworth.identity.domain.AppUserStatus;
import com.workworth.identity.persistence.AppUserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkdayLifecycleScheduler {
    private final WorkdayService service;
    private final AppUserRepository users;

    public WorkdayLifecycleScheduler(WorkdayService service, AppUserRepository users) {
        this.service = service;
        this.users = users;
    }

    @Scheduled(fixedDelay = 60000)
    public void reconcileToday() {
        try {
            users.findAllByStatus(AppUserStatus.ACTIVE).forEach(service::reconcileThroughToday);
        } catch (RuntimeException ignored) {
        }
    }
}
