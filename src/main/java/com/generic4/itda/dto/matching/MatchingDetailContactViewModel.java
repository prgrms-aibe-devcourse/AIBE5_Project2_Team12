package com.generic4.itda.dto.matching;

public record MatchingDetailContactViewModel(
        boolean contactVisible,
        MatchingParticipantContactViewModel client,
        MatchingParticipantContactViewModel freelancer
) {
}
