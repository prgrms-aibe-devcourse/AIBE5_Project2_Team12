package com.generic4.itda.dto.matching;

import java.time.LocalDateTime;
import java.util.List;

public record MatchingDetailCancellationViewModel(
        boolean requested,
        boolean requestedByCurrentUser,
        boolean canRequest,
        boolean canWithdraw,
        boolean canConfirm,
        String requesterRoleLabel,
        String receiverRoleLabel,
        String reasonLabel,
        String reasonDetail,
        LocalDateTime requestedAt,
        LocalDateTime autoCancelAt,
        List<MatchingCancellationReasonOptionViewModel> reasonOptions
) {
}
