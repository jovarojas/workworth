package com.workworth.workday.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.workworth.workday.domain.*;
import com.workworth.workday.exception.WorkdayConflictException;
import com.workworth.workday.exception.WorkdayIntervalValidationException;
import com.workworth.workday.persistence.*;
import java.time.*; import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class WorkdayServiceTest {
 private WorkdayRepository workdays=mock(WorkdayRepository.class); private MealBreakRepository breaks=mock(MealBreakRepository.class); private PartialAbsenceRepository absences=mock(PartialAbsenceRepository.class); private WorkdayTimeCorrectionRepository corrections=mock(WorkdayTimeCorrectionRepository.class); private final List<MealBreak> breakList=new ArrayList<>(); private final List<PartialAbsence> absenceList=new ArrayList<>();
 private Clock clock; private WorkdayService service;
 @BeforeEach void init(){clock=Clock.fixed(Instant.parse("2026-07-06T08:00:00Z"),ZoneId.of("Europe/Madrid")); service=new WorkdayService(workdays,breaks,absences,corrections,new EconomicTimeCalculator(),clock,"Europe/Madrid"); when(breaks.findByWorkdayIdOrderByStartedAt(any())).thenAnswer(x->breakList); when(absences.findByWorkdayIdOrderByStartedAt(any())).thenAnswer(x->absenceList); when(breaks.save(any())).thenAnswer(x->{MealBreak b=x.getArgument(0);breakList.add(b);return b;}); when(absences.save(any())).thenAnswer(x->{PartialAbsence a=x.getArgument(0);absenceList.add(a);return a;}); }
 private Workday day(LocalDate date){var s=WorkdaySchedule.forDate(date).orElseThrow();return new Workday(date,"Europe/Madrid",s.variant(),s.start(),s.end(),s.maximumEconomicTime().getSeconds(),clock.instant());}
 private void existing(Workday d){ReflectionTestUtils.setField(d,"id",1L);when(workdays.findLockedByLocalDate(d.getLocalDate())).thenReturn(Optional.of(d));}
 @Test void rejectsPausePauseAndPauseAbsenceOverlaps(){var d=day(LocalDate.of(2026,7,6));existing(d); service.startMealBreak(d.getLocalDate()); assertThatThrownBy(()->service.startMealBreak(d.getLocalDate())).isInstanceOf(WorkdayConflictException.class); breakList.clear(); absenceList.add(new PartialAbsence(d,Instant.parse("2026-07-06T07:00:00Z"),Instant.parse("2026-07-06T09:00:00Z"),null)); assertThatThrownBy(()->service.startMealBreak(d.getLocalDate())).isInstanceOf(WorkdayIntervalValidationException.class);}
 @Test void rejectsAbsenceAbsenceOverlapAndAllowsUpdate(){var d=day(LocalDate.of(2026,7,6));existing(d); var first=service.addAbsence(d.getLocalDate(),Instant.parse("2026-07-06T07:00:00Z"),Instant.parse("2026-07-06T08:00:00Z"),"doctor"); ReflectionTestUtils.setField(first,"id",2L); assertThatThrownBy(()->service.addAbsence(d.getLocalDate(),Instant.parse("2026-07-06T07:30:00Z"),Instant.parse("2026-07-06T08:30:00Z"),null)).isInstanceOf(WorkdayIntervalValidationException.class); when(absences.findById(first.getId())).thenReturn(Optional.of(first)); service.updateAbsence(d.getLocalDate(),first.getId(),Instant.parse("2026-07-06T09:00:00Z"),Instant.parse("2026-07-06T10:00:00Z"),"updated"); assertThat(first.getReason()).isEqualTo("updated");}
 @Test void reconcilesStatesCancelsAndAutoClosesBreak(){var scheduled=day(LocalDate.of(2026,7,7));existing(scheduled);assertThat(service.reconcile(scheduled.getLocalDate()).getStatus()).isEqualTo(WorkdayStatus.SCHEDULED); var active=day(LocalDate.of(2026,7,6));existing(active);service.cancel(active.getLocalDate(),"holiday");assertThat(active.getStatus()).isEqualTo(WorkdayStatus.CANCELLED); var done=day(LocalDate.of(2026,7,3));existing(done);breakList.add(new MealBreak(done,Instant.parse("2026-07-03T12:00:00Z")));service.reconcile(done.getLocalDate());assertThat(done.getStatus()).isEqualTo(WorkdayStatus.COMPLETED);assertThat(breakList.get(0).isEndedAutomatically()).isTrue();}
 @Test void reconcilesSameDateIdempotently(){var d=day(LocalDate.of(2026,7,6));existing(d);assertThat(service.reconcile(d.getLocalDate())).isSameAs(service.reconcile(d.getLocalDate()));verify(workdays,never()).save(any());}
}
