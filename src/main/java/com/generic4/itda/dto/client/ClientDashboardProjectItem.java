package com.generic4.itda.dto.client;

public record ClientDashboardProjectItem(
        Long proposalId,
        String title,
        String statusKey,
        String statusLabel,
        int positionCount,
        String totalBudgetText,
        String modifiedDate,
        String matchingOverview
) {
}
