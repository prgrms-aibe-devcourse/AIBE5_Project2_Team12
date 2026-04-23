package com.generic4.itda.dto.matching;

import com.generic4.itda.domain.matching.constant.MatchingStatus;
import java.util.List;

public record ClientMatchingFreelancerSelectionViewModel(
        Long proposalId,
        String proposalTitle,
        Long proposalPositionId,
        String proposalPositionTitle,
        String positionStatusLabel,
        String filterStatusKey,
        List<MatchingStatus> availableStatuses,
        List<ClientMatchingFreelancerItemViewModel> items
) {
}

