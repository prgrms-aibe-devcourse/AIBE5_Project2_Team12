package com.generic4.itda.dto.matching;

public record MatchingParticipantContactViewModel(
        String roleLabel,
        String displayName,
        String email,
        String phone
) {
}
