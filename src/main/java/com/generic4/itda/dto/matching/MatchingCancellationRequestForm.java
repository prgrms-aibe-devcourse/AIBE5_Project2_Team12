package com.generic4.itda.dto.matching;

import com.generic4.itda.domain.matching.constant.MatchingCancellationReason;

public record MatchingCancellationRequestForm(
        MatchingCancellationReason reason,
        String reasonDetail
) {
}
