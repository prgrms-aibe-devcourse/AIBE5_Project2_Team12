package com.generic4.itda.dto.matching;

import com.generic4.itda.domain.matching.constant.MatchingStatus;
import java.util.List;

public record MatchingDetailViewModel(
        Long matchingId,
        String viewerRole,
        MatchingStatus status,
        boolean contactVisible,
        MatchingDetailHeaderViewModel header,
        MatchingDetailSummaryViewModel summary,
        MatchingDetailContactViewModel contacts,
        MatchingDetailProjectSummaryViewModel project,
        List<MatchingTimelineItemViewModel> timeline
) {
}
