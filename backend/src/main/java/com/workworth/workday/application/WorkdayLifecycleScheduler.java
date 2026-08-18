package com.workworth.workday.application;

import java.time.Clock;
import java.time.LocalDate;

import com.workworth.identity.domain.AppUserStatus;
import com.workworth.identity.persistence.AppUserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkdayLifecycleScheduler {
    private final WorkdayService service;
    private final Clock clock;
    private final AppUserRepository users;

    public WorkdayLifecycleScheduler(WorkdayService service, Clock clock, AppUserRepository users) {
        this.service = service;
        this.clock = clock;
        this.users = users;
    }

    @Scheduled(fixedDelay = 60000)
    public void reconcileToday() {
        try {
            users.findAllByStatus(AppUserStatus.ACTIVE).forEach(user ->
                service.reconcile(user, LocalDate.now(clock.withZone(java.time.ZoneId.of(user.getTimeZone())))));
        } catch (RuntimeException ignored) {
        }
    }
}
