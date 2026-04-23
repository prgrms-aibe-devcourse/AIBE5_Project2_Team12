package com.generic4.itda.dto.matching;

import java.util.List;

public record ClientMatchingListViewModel(
        Long proposalId,
        String proposalTitle,
        List<PositionFilterItem> positionFilters,
        Long selectedPositionId,
        String selectedStatus,
        List<ClientMatchingListItemViewModel> items
) {
    public record PositionFilterItem(
            Long proposalPositionId,
            String title,
            long count
    ) {
    }
}
