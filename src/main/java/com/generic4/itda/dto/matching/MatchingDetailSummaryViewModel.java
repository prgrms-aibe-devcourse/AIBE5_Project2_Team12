package com.generic4.itda.dto.matching;

import java.time.LocalDateTime;

public record MatchingDetailSummaryViewModel(
        String headline,
        String helperMessage,
        LocalDateTime requestedAt,
        LocalDateTime respondedAt,
        String contactGuideMessage
) {
}
