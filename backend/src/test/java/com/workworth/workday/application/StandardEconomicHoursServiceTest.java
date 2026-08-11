package com.workworth.workday.application;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
class StandardEconomicHoursServiceTest {
 private final StandardEconomicHoursService service=new StandardEconomicHoursService();
 @Test void countsNormalSummerAndLeapYearMonths() {
  assertThat(service.getStandardEconomicHours(YearMonth.of(2026,7),ZoneId.of("Europe/Madrid"))).isEqualByComparingTo("161");
  assertThat(service.getStandardEconomicHours(YearMonth.of(2026,8),ZoneId.of("Europe/Madrid"))).isEqualByComparingTo("147");
  assertThat(service.getStandardEconomicHours(YearMonth.of(2024,2),ZoneId.of("Europe/Madrid"))).isPositive();
 }
}
