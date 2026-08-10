package com.workworth.salary.estimation;

import java.util.Optional;

public interface FiscalNetEstimatorRegistry {

    Optional<FiscalNetEstimator> findSupported(FiscalEstimateContext context);
}
