package com.workworth.common.schedule;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;

public interface StandardEconomicHoursProvider {

    BigDecimal getStandardEconomicHours(YearMonth month, ZoneId zoneId);
}
