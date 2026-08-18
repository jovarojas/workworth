package com.workworth.workday.domain;

/**
 * Workday-owned fact published when a workday first reaches its scheduled completion.
 */
public record WorkdayCompletedEvent(Long workdayId) {
}
