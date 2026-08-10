package com.workworth.salary.estimation;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NoFiscalNetEstimatorRegistry implements FiscalNetEstimatorRegistry {

    @Override
    public Optional<FiscalNetEstimator> findSupported(FiscalEstimateContext context) {
        return Optional.empty();
    }
}
