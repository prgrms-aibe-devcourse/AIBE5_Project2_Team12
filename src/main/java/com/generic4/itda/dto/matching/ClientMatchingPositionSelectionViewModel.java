package com.generic4.itda.dto.matching;

import java.util.List;

public record ClientMatchingPositionSelectionViewModel(
        Long proposalId,
        String proposalTitle,
        boolean openOnly,
        List<ClientMatchingPositionItemViewModel> positions
) {
}

