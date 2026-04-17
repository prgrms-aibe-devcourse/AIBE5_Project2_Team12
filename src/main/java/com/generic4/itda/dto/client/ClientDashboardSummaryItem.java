package com.generic4.itda.dto.client;

public record ClientDashboardSummaryItem(
        String filterKey,
        String title,
        long count,
        String description,
        boolean selected
) {
}
