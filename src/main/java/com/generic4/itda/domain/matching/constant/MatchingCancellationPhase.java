package com.generic4.itda.domain.matching.constant;

import lombok.Getter;

@Getter
public enum MatchingCancellationPhase {
    BEFORE_CONTRACT("계약 시작 전"),
    AFTER_CONTRACT("계약 진행 중");

    private final String label;

    MatchingCancellationPhase(String label) {
        this.label = label;
    }
}
