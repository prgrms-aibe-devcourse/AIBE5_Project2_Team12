package com.generic4.itda.domain.proposal;

import lombok.Getter;

@Getter
public enum ProposalWorkType {
    SITE("상주"),
    REMOTE("원격"),
    HYBRID("상주, 원격 모두 가능");

    private final String description;

    ProposalWorkType(String description) {
        this.description = description;
    }
}
