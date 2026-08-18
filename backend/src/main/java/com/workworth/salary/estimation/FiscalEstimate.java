package com.workworth.salary.estimation;

import java.math.BigDecimal;

public record FiscalEstimate(
    BigDecimal annualNet,
    BigDecimal monthlyNet,
    int fiscalYear,
    String ruleSetVersion,
    String inputSnapshot) {
}
