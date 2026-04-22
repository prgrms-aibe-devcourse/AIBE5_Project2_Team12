package com.generic4.itda.dto.matching;

import java.time.LocalDateTime;

public record MatchingTimelineItemViewModel(
        LocalDateTime occurredAt,
        String actorLabel,
        String actionLabel,
        String description
) {
}
