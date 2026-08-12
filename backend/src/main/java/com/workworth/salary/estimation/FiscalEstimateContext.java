package com.workworth.salary.estimation;

import java.math.BigDecimal;

public record FiscalEstimateContext(
    int fiscalYear,
    String countryCode,
    String regionCode,
    BigDecimal grossAnnual,
    int payPeriods,
    String contractType,
    String familyStatus,
    int dependentChildren,
    boolean disability,
    boolean otherIncome,
    String currencyCode) {
}
