package com.generic4.itda.domain.proposal;

import lombok.Getter;

@Getter
public enum ProposalPositionSkillImportance {
    PREFERENCE("우대"),
    ESSENTIAL("필수");

    private final String description;

    ProposalPositionSkillImportance(String description) {
        this.description = description;
    }
}
