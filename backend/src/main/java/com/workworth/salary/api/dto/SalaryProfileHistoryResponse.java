package com.workworth.salary.api.dto;

import java.util.List;

public record SalaryProfileHistoryResponse(
        List<SalaryProfileResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
