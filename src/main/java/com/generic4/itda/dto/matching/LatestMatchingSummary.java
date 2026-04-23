package com.generic4.itda.dto.matching;

import com.generic4.itda.domain.matching.constant.MatchingStatus;

public record LatestMatchingSummary(
        Long matchingId,
        MatchingStatus status
) {
}

