package com.workworth.workday.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.workworth.workday.domain.*;
import com.workworth.workday.exception.WorkdayConflictException;
import com.workworth.workday.exception.WorkdayIntervalValidationException;
import com.workworth.workday.persistence.*;
import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;
import java.time.*; import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

class WorkdayServiceTest {
 private WorkdayRepository workdays=mock(WorkdayRepository.class); private MealBreakRepository breaks=mock(MealBreakRepository.class); private PartialAbsenceRepository absences=mock(PartialAbsenceRepository.class); private WorkdayTimeCorrectionRepository corrections=mock(WorkdayTimeCorrectionRepository.class); private AppUserRepository users=mock(AppUserRepository.class); private final List<MealBreak> breakList=new ArrayList<>(); private final List<PartialAbsence> absenceList=new ArrayList<>();
 private Clock clock; private WorkdayService service; private ApplicationEventPublisher events=mock(ApplicationEventPublisher.class); private CurrentUserProvider currentUser=mock(CurrentUserProvider.class); private AppUser user;
 // createdAt is pinned to "today" (same local date as the fixed clock) so this shared fixture
 // represents a user with no back-fill history to reconcile; tests that need an older sign-up
 // date build their own AppUser instead of reusing this one.
 @BeforeEach void init(){clock=Clock.fixed(Instant.parse("2026-07-06T08:00:00Z"),ZoneId.of("Europe/Madrid")); user=new AppUser(UUID.randomUUID(),"test|workday","workday@test.invalid","Europe/Madrid",Instant.parse("2026-07-06T05:00:00Z"));when(currentUser.currentUser()).thenReturn(user); service=new WorkdayService(workdays,breaks,absences,corrections,new EconomicTimeCalculator(),clock,events,currentUser,users); when(breaks.findByWorkdayIdOrderByStartedAt(any())).thenAnswer(x->breakList); when(absences.findByWorkdayIdOrderByStartedAt(any())).thenAnswer(x->absenceList); when(breaks.save(any())).thenAnswer(x->{MealBreak b=x.getArgument(0);breakList.add(b);return b;}); when(absences.save(any())).thenAnswer(x->{PartialAbsence a=x.getArgument(0);absenceList.add(a);return a;}); when(corrections.save(any())).thenAnswer(x->{WorkdayTimeCorrection correction=x.getArgument(0);ReflectionTestUtils.setField(correction,"id",99L);return correction;}); }
 private Workday day(LocalDate date){var s=WorkdaySchedule.forDate(date).orElseThrow();return new Workday(user,date,"Europe/Madrid",s.variant(),s.start(),s.end(),s.maximumEconomicTime().getSeconds(),clock.instant());}
 private void existing(Workday d){ReflectionTestUtils.setField(d,"id",1L);when(workdays.findLockedByUserIdAndLocalDate(user.getId(),d.getLocalDate())).thenReturn(Optional.of(d));}
 @Test void rejectsPausePauseAndPauseAbsenceOverlaps(){var d=day(LocalDate.of(2026,7,6));existing(d); service.startMealBreak(d.getLocalDate()); assertThatThrownBy(()->service.startMealBreak(d.getLocalDate())).isInstanceOf(WorkdayConflictException.class); breakList.clear(); absenceList.add(new PartialAbsence(d,Instant.parse("2026-07-06T07:00:00Z"),Instant.parse("2026-07-06T09:00:00Z"),null)); assertThatThrownBy(()->service.startMealBreak(d.getLocalDate())).isInstanceOf(WorkdayIntervalValidationException.class);}
 @Test void rejectsAbsenceAbsenceOverlapAndAllowsUpdate(){var d=day(LocalDate.of(2026,7,6));existing(d); var first=service.addAbsence(d.getLocalDate(),Instant.parse("2026-07-06T07:00:00Z"),Instant.parse("2026-07-06T08:00:00Z"),"doctor"); ReflectionTestUtils.setField(first,"id",2L); assertThatThrownBy(()->service.addAbsence(d.getLocalDate(),Instant.parse("2026-07-06T07:30:00Z"),Instant.parse("2026-07-06T08:30:00Z"),null)).isInstanceOf(WorkdayIntervalValidationException.class); when(absences.findById(first.getId())).thenReturn(Optional.of(first)); service.updateAbsence(d.getLocalDate(),first.getId(),Instant.parse("2026-07-06T09:00:00Z"),Instant.parse("2026-07-06T10:00:00Z"),"updated"); assertThat(first.getReason()).isEqualTo("updated");}
 @Test void reconcilesStatesCancelsAndAutoClosesBreak(){var scheduled=day(LocalDate.of(2026,7,7));existing(scheduled);assertThat(service.reconcile(scheduled.getLocalDate()).getStatus()).isEqualTo(WorkdayStatus.SCHEDULED);service.cancel(scheduled.getLocalDate(),"holiday");assertThat(scheduled.getStatus()).isEqualTo(WorkdayStatus.CANCELLED); var active=day(LocalDate.of(2026,7,6));existing(active);service.cancel(active.getLocalDate(),"holiday");assertThat(active.getStatus()).isEqualTo(WorkdayStatus.CANCELLED); var done=day(LocalDate.of(2026,7,3));existing(done);breakList.add(new MealBreak(done,Instant.parse("2026-07-03T12:00:00Z")));service.reconcile(done.getLocalDate());assertThat(done.getStatus()).isEqualTo(WorkdayStatus.COMPLETED);assertThat(breakList.get(0).isEndedAutomatically()).isTrue();service.cancel(done.getLocalDate(),"holiday");assertThat(done.getStatus()).isEqualTo(WorkdayStatus.CANCELLED);}
 @Test void reconcilesSameDateIdempotently(){var d=day(LocalDate.of(2026,7,6));existing(d);assertThat(service.reconcile(d.getLocalDate())).isSameAs(service.reconcile(d.getLocalDate()));verify(workdays,never()).save(any());}
 @Test void locksTheUserDateBeforeCreatingTheWorkday(){var date=LocalDate.of(2026,7,6);var d=day(date);when(workdays.findLockedByUserIdAndLocalDate(user.getId(),date)).thenReturn(Optional.empty());when(workdays.save(any())).thenReturn(d);assertThat(service.reconcile(date)).isSameAs(d);verify(workdays).lockUserDate(user.getId(),date);}
 @Test void amendsOnlyClosedBreaksOnCompletedWorkdaysAndRecordsAudit(){var completed=day(LocalDate.of(2026,7,3));existing(completed);var breakToAmend=new MealBreak(completed,Instant.parse("2026-07-03T08:00:00Z"));breakToAmend.end(Instant.parse("2026-07-03T09:00:00Z"),true);ReflectionTestUtils.setField(breakToAmend,"id",2L);breakList.add(breakToAmend);when(breaks.findById(2L)).thenReturn(Optional.of(breakToAmend));service.amendHistoricalMealBreak(completed.getLocalDate(),2L,Instant.parse("2026-07-03T07:00:00Z"),Instant.parse("2026-07-03T09:00:00Z"));var captured=ArgumentCaptor.forClass(WorkdayTimeCorrection.class);verify(corrections).save(captured.capture());assertThat(captured.getValue().getCause()).isEqualTo(WorkdayTimeCorrectionCause.MEAL_BREAK_CHANGED);assertThat(captured.getValue().getMealBreak()).isSameAs(breakToAmend);assertThat(captured.getValue().getPreviousBreakStartedAt()).isEqualTo(Instant.parse("2026-07-03T08:00:00Z"));assertThat(captured.getValue().getNewBreakStartedAt()).isEqualTo(Instant.parse("2026-07-03T07:00:00Z"));assertThat(captured.getValue().getPreviousBreakEndedAutomatically()).isTrue();assertThat(breakToAmend.isEndedAutomatically()).isFalse();verify(events).publishEvent(any(WorkdayTimeCorrectionRegisteredEvent.class));}
 @Test void rejectsHistoricalBreakAmendmentsForAllNonCompletedStates(){var scheduled=day(LocalDate.of(2026,7,7));existing(scheduled);assertThatThrownBy(()->service.amendHistoricalMealBreak(scheduled.getLocalDate(),2L,Instant.EPOCH,Instant.EPOCH.plusSeconds(1))).isInstanceOf(WorkdayConflictException.class);var active=day(LocalDate.of(2026,7,6));existing(active);assertThatThrownBy(()->service.amendHistoricalMealBreak(active.getLocalDate(),2L,Instant.EPOCH,Instant.EPOCH.plusSeconds(1))).isInstanceOf(WorkdayConflictException.class);breakList.add(new MealBreak(active,Instant.parse("2026-07-06T08:00:00Z")));assertThatThrownBy(()->service.amendHistoricalMealBreak(active.getLocalDate(),2L,Instant.EPOCH,Instant.EPOCH.plusSeconds(1))).isInstanceOf(WorkdayConflictException.class);var cancelled=day(LocalDate.of(2026,7,3));cancelled.cancel("holiday",clock.instant());existing(cancelled);assertThatThrownBy(()->service.amendHistoricalMealBreak(cancelled.getLocalDate(),2L,Instant.EPOCH,Instant.EPOCH.plusSeconds(1))).isInstanceOf(WorkdayConflictException.class);}
 @Test void rejectsInvalidOutOfWindowAndOverlappingHistoricalBreakAmendments(){var completed=day(LocalDate.of(2026,7,3));existing(completed);var target=new MealBreak(completed,Instant.parse("2026-07-03T07:00:00Z"));target.end(Instant.parse("2026-07-03T08:00:00Z"),false);ReflectionTestUtils.setField(target,"id",2L);breakList.add(target);when(breaks.findById(2L)).thenReturn(Optional.of(target));assertThatThrownBy(()->service.amendHistoricalMealBreak(completed.getLocalDate(),2L,Instant.parse("2026-07-03T08:00:00Z"),Instant.parse("2026-07-03T07:00:00Z"))).isInstanceOf(WorkdayIntervalValidationException.class);assertThatThrownBy(()->service.amendHistoricalMealBreak(completed.getLocalDate(),2L,Instant.parse("2026-07-03T05:00:00Z"),Instant.parse("2026-07-03T07:00:00Z"))).isInstanceOf(WorkdayIntervalValidationException.class);var other=new MealBreak(completed,Instant.parse("2026-07-03T09:00:00Z"));other.end(Instant.parse("2026-07-03T10:00:00Z"),false);ReflectionTestUtils.setField(other,"id",3L);breakList.add(other);assertThatThrownBy(()->service.amendHistoricalMealBreak(completed.getLocalDate(),2L,Instant.parse("2026-07-03T08:30:00Z"),Instant.parse("2026-07-03T09:30:00Z"))).isInstanceOf(WorkdayIntervalValidationException.class);breakList.remove(other);absenceList.add(new PartialAbsence(completed,Instant.parse("2026-07-03T09:00:00Z"),Instant.parse("2026-07-03T10:00:00Z"),null));assertThatThrownBy(()->service.amendHistoricalMealBreak(completed.getLocalDate(),2L,Instant.parse("2026-07-03T08:30:00Z"),Instant.parse("2026-07-03T09:30:00Z"))).isInstanceOf(WorkdayIntervalValidationException.class);}

 @Test void reconcileThroughTodaySkipsBackfillForAUserCreatedTodayWithoutAnyPriorWorkday(){
  when(workdays.save(any())).thenAnswer(x->x.getArgument(0));
  Workday result = service.reconcileThroughToday(user);
  assertThat(result.getLocalDate()).isEqualTo(LocalDate.of(2026,7,6));
  verify(workdays).findLatestLocalDate(user.getId());
  verify(workdays,times(1)).save(any());
 }

 @Test void reconcileThroughTodayBackfillsPendingWeekdaysSinceTheLastKnownWorkdaySkippingTheWeekend(){
  when(workdays.findLatestLocalDate(user.getId())).thenReturn(Optional.of(LocalDate.of(2026,7,1)));
  when(workdays.save(any())).thenAnswer(x->x.getArgument(0));
  ArgumentCaptor<Workday> captor = ArgumentCaptor.forClass(Workday.class);

  Workday result = service.reconcileThroughToday(user);

  verify(workdays, times(3)).save(captor.capture());
  List<LocalDate> createdDates = captor.getAllValues().stream().map(Workday::getLocalDate).toList();
  assertThat(createdDates).containsExactly(LocalDate.of(2026,7,2), LocalDate.of(2026,7,3), LocalDate.of(2026,7,6));
  assertThat(result.getLocalDate()).isEqualTo(LocalDate.of(2026,7,6));
 }

 // Regression test for BUG 1's remaining gap: a user who signed up several days before her
 // first-ever login must not lose the working days between sign-up and that first login. Since
 // she has no Workday yet, the anchor must fall back to AppUser#getCreatedAt() (2026-07-01,
 // a Wednesday), not to "today".
 @Test void reconcileThroughTodayBackfillsFromAccountCreationForAUserWithoutAnyPriorWorkday(){
  AppUser createdDaysAgo = new AppUser(UUID.randomUUID(), "test|workday-late-open", "late-open@test.invalid",
      "Europe/Madrid", Instant.parse("2026-07-01T09:00:00Z"));
  when(workdays.save(any())).thenAnswer(x->x.getArgument(0));
  ArgumentCaptor<Workday> captor = ArgumentCaptor.forClass(Workday.class);

  Workday result = service.reconcileThroughToday(createdDaysAgo);

  verify(workdays, times(4)).save(captor.capture());
  List<LocalDate> createdDates = captor.getAllValues().stream().map(Workday::getLocalDate).toList();
  // 2026-07-01 (creation day itself, Wed) through 2026-07-03 (Fri), then today (07-06, Mon);
  // 07-04/07-05 is the weekend and nothing before 07-01 (createdAt) is ever touched.
  assertThat(createdDates).containsExactly(
      LocalDate.of(2026,7,1), LocalDate.of(2026,7,2), LocalDate.of(2026,7,3), LocalDate.of(2026,7,6));
  assertThat(createdDates).allMatch(date -> !date.isBefore(LocalDate.of(2026,7,1)));
  assertThat(result.getLocalDate()).isEqualTo(LocalDate.of(2026,7,6));
 }

 @Test void reconcileThroughTodayUsesTheUsersTimeZoneNotUtcToComputeToday(){
  Clock nearMidnightUtc = Clock.fixed(Instant.parse("2026-07-07T02:00:00Z"), ZoneOffset.UTC);
  WorkdayService zonedService = new WorkdayService(workdays, breaks, absences, corrections, new EconomicTimeCalculator(), nearMidnightUtc, events, currentUser, users);
  // createdAt sits on the same Pacific-local day as "today" so this test isolates the timezone
  // conversion for `current()`/`reconcileThroughToday()`, without also exercising the backfill.
  AppUser pacificUser = new AppUser(UUID.randomUUID(), "test|workday-tz", "tz@test.invalid", "America/Los_Angeles", Instant.parse("2026-07-06T15:00:00Z"));
  when(currentUser.currentUser()).thenReturn(pacificUser);
  when(workdays.save(any())).thenAnswer(x->x.getArgument(0));

  Workday result = zonedService.current();

  assertThat(result.getLocalDate()).isEqualTo(LocalDate.of(2026,7,6));
 }
}
