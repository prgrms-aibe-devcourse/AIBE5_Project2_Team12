package com.generic4.itda.domain.recommendation;

import lombok.Getter;

@Getter
public enum RecommendationRunStatus {
    PENDING("대기중"), RUNNING("실행중"), COMPUTED("계산 완료"), FAILED("실패");

    private final String description;

    RecommendationRunStatus(String description) {
        this.description = description;
    }
}
