package com.workworth.earnings.application;

import java.util.List;

public record EarningHistoryPage(
    List<EffectiveEarning> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious) {
}
