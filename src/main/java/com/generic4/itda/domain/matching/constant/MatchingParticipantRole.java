package com.generic4.itda.domain.matching.constant;

import lombok.Getter;

@Getter
public enum MatchingParticipantRole {
    CLIENT("클라이언트"),
    FREELANCER("프리랜서");

    private final String label;

    MatchingParticipantRole(String label) {
        this.label = label;
    }
}
