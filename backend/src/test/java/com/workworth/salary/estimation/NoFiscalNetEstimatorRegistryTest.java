package com.workworth.salary.estimation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NoFiscalNetEstimatorRegistryTest {

    @Test
    void doesNotProvideAnEstimatorBeforeFiscalRulesAreImplemented() {
        FiscalEstimateContext context = new FiscalEstimateContext(
                2026, "ES", "ES-VC", new BigDecimal("19000.00"), 12,
                "INDEFINITE", "SINGLE", 0, false, false, "EUR");

        assertThat(new NoFiscalNetEstimatorRegistry().findSupported(context)).isEmpty();
    }
}
