package com.workworth.salary.api.dto;

/**
 * A salary change already scheduled for a future month, distinct from the currently effective
 * profile returned by {@code /salary-profiles/current}. {@code salaryProfile} is {@code null}
 * when the user has no change scheduled beyond the current month.
 */
public record UpcomingSalaryProfileResponse(SalaryProfileResponse salaryProfile) {
}
