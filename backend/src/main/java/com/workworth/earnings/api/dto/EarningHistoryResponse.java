package com.workworth.earnings.api.dto;

import com.workworth.earnings.application.EarningHistoryPage;
import java.util.List;

public record EarningHistoryResponse(
        List<EarningResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {

    public static EarningHistoryResponse from(EarningHistoryPage history) {
        return new EarningHistoryResponse(history.items().stream().map(EarningResponse::from).toList(),
                history.page(), history.size(), history.totalElements(), history.totalPages(), history.hasNext(),
                history.hasPrevious());
    }
}
