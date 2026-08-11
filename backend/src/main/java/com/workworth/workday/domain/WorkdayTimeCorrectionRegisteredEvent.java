package com.workworth.workday.domain;

/**
 * Signals that a persisted workday-time correction may require a downstream
 * economic revision. It intentionally carries no monetary information.
 */
public record WorkdayTimeCorrectionRegisteredEvent(Long workdayTimeCorrectionId) {
}
