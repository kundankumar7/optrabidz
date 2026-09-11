package com.project.optrabidz.common.application.pagination;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
    public PageResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
