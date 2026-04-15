package com.generic4.itda.domain.proposal;

import lombok.Getter;

@Getter
public enum ProposalStatus {
    WRITING("작성 중"),
    MATCHING("모집/추천 진행 중"),
    COMPLETE("종료");

    private final String description;

    ProposalStatus(String description) {
        this.description = description;
    }
}
