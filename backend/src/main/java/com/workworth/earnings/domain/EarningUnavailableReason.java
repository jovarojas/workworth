package com.workworth.earnings.domain;

/**
 * Stable reasons for an earning that cannot be valued without inventing a salary rate.
 */
public enum EarningUnavailableReason {
    SALARY_PROFILE_NOT_FOUND,
    SALARY_CONFIGURATION_INCOMPLETE,
    SALARY_RATE_UNAVAILABLE,
    // Not a salary problem at all: the date simply has no standard workday (e.g. a weekend).
    // Kept separate from the salary-related reasons above so the UI can phrase it accordingly.
    NOT_A_WORKDAY
}
