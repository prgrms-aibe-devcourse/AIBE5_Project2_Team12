package com.generic4.itda.dto.client;

import java.util.List;

public record ClientDashboardViewModel(
        String selectedFilterKey,
        String selectedFilterTitle,
        List<ClientDashboardSummaryItem> summaries,
        List<ClientDashboardProjectItem> projects
) {
}
