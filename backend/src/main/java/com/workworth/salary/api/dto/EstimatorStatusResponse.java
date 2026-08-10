package com.workworth.salary.api.dto;

import com.workworth.salary.domain.EstimatorStatus;
import java.util.List;

public record EstimatorStatusResponse(
        int fiscalYear,
        EstimatorStatus status,
        List<String> requiredInputs) {
}
