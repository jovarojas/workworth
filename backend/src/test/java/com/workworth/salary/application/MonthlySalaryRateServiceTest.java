package com.workworth.salary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.workworth.common.schedule.StandardEconomicHoursProvider;
import com.workworth.salary.domain.MonthlySalaryRate;
import com.workworth.salary.exception.SalaryRateUnavailableException;
import com.workworth.salary.persistence.SalaryProfile;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.Mockito;

class MonthlySalaryRateServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-10T08:00:00Z"), ZoneId.of("Europe/Madrid"));

    @Test
    void calculatesRateWithHoursProvidedByScheduleModule() {
        SalaryProfileService profileService = Mockito.mock(SalaryProfileService.class);
        StandardEconomicHoursProvider hoursProvider = (month, zoneId) -> new BigDecimal("168");
        ObjectProvider<StandardEconomicHoursProvider> provider = providerOf(hoursProvider);
        SalaryProfile profile = new SalaryProfile(
                YearMonth.of(2026, 8).atDay(1), new BigDecimal("19000.00"), new BigDecimal("1260.00"),
                "EUR", 12, clock.instant());
        when(profileService.findEffectiveProfile(YearMonth.of(2026, 8))).thenReturn(profile);

        MonthlySalaryRate rate = new MonthlySalaryRateService(profileService, provider, clock)
                .getRate(YearMonth.of(2026, 8));

        assertThat(rate.hourlyNetRate()).isEqualByComparingTo("7.500000000000");
    }

    @Test
    void reportsRateUnavailableWithoutScheduleImplementation() {
        SalaryProfileService profileService = Mockito.mock(SalaryProfileService.class);
        SalaryProfile profile = new SalaryProfile(
                YearMonth.of(2026, 8).atDay(1), null, new BigDecimal("1260.00"), "EUR", 12, clock.instant());
        when(profileService.findEffectiveProfile(YearMonth.of(2026, 8))).thenReturn(profile);

        assertThatThrownBy(() -> new MonthlySalaryRateService(profileService, providerOf(null), clock)
                .getRate(YearMonth.of(2026, 8)))
                .isInstanceOf(SalaryRateUnavailableException.class)
                .hasMessageContaining("SPEC 002");
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<StandardEconomicHoursProvider> providerOf(StandardEconomicHoursProvider provider) {
        ObjectProvider<StandardEconomicHoursProvider> objectProvider = Mockito.mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        return objectProvider;
    }
}
