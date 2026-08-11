package com.workworth.workday.application;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component public class WorkdayLifecycleScheduler {
 private final WorkdayService service; private final Clock clock;
 public WorkdayLifecycleScheduler(WorkdayService service, Clock clock){this.service=service;this.clock=clock;}
 @Scheduled(fixedDelay = 60000) public void reconcileToday(){ try { service.reconcile(LocalDate.now(clock)); } catch (RuntimeException ignored) { } }
}
