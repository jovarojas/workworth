package com.workworth.earnings.domain;

/** Stable reasons for an earning that cannot be valued without inventing a salary rate. */
public enum EarningUnavailableReason {
    SALARY_PROFILE_NOT_FOUND,
    SALARY_CONFIGURATION_INCOMPLETE,
    SALARY_RATE_UNAVAILABLE
}
