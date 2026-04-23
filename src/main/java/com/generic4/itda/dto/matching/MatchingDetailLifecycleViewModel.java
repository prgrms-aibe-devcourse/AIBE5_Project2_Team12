package com.generic4.itda.dto.matching;

public record MatchingDetailLifecycleViewModel(
        String currentUserRoleLabel,
        String counterpartRoleLabel,
        boolean clientContractAccepted,
        boolean freelancerContractAccepted,
        boolean currentUserContractAccepted,
        boolean counterpartContractAccepted,
        boolean canAcceptContractStart,
        boolean contractCancellation,
        MatchingDetailCancellationViewModel cancellation,
        boolean clientReviewed,
        boolean freelancerReviewed,
        boolean currentUserReviewed,
        boolean counterpartReviewed,
        boolean canSubmitReview,
        String currentUserReview,
        String counterpartReview,
        boolean counterpartReviewVisible,
        boolean currentUserCompletionConfirmed,
        boolean counterpartCompletionConfirmed,
        boolean canConfirmCompletion
) {
}
