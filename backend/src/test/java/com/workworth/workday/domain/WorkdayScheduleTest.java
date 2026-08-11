package com.workworth.workday.domain;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
class WorkdayScheduleTest {
 @Test void distinguishesNormalFridayAndSummerSchedules() {
  assertThat(WorkdaySchedule.forDate(LocalDate.of(2026, 10, 5)).orElseThrow().maximumEconomicTime().toHours()).isEqualTo(8);
  assertThat(WorkdaySchedule.forDate(LocalDate.of(2026, 10, 2)).orElseThrow().maximumEconomicTime().toHours()).isEqualTo(7);
  assertThat(WorkdaySchedule.forDate(LocalDate.of(2026, 7, 6)).orElseThrow().variant()).isEqualTo(ScheduleVariant.SUMMER);
  assertThat(WorkdaySchedule.forDate(LocalDate.of(2026, 7, 6)).orElseThrow().maximumEconomicTime().toHours()).isEqualTo(7);
  assertThat(WorkdaySchedule.forDate(LocalDate.of(2026, 7, 4))).isEmpty();
 }
}
