package com.workworth.salary.api.dto;

import java.time.YearMonth;

public record CurrentSalaryProfileResponse(
    YearMonth month,
    SalaryProfileResponse salaryProfile) {
}
