package com.generic4.itda.dto.matching;

import com.generic4.itda.domain.matching.constant.MatchingStatus;
import java.time.LocalDateTime;

public record ClientMatchingListItemViewModel(
        Long matchingId,
        Long proposalPositionId,
        String positionTitle,
        String freelancerName,
        MatchingStatus status,
        String statusLabel,
        LocalDateTime requestedAt
) {
}
