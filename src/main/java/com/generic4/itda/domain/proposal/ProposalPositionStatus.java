package com.generic4.itda.domain.proposal;

import lombok.Getter;

@Getter
public enum ProposalPositionStatus {
    OPEN("모집 중"),
    FULL("정원 충족"),
    CLOSED("모집 종료");

    private final String description;

    ProposalPositionStatus(String description) {
        this.description = description;
    }
}
