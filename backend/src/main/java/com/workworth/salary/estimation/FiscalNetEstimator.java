package com.workworth.salary.estimation;

public interface FiscalNetEstimator {

    boolean supports(FiscalEstimateContext context);

    FiscalEstimate calculate(FiscalEstimateContext context);
}
