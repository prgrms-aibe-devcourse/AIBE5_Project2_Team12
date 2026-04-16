package com.generic4.itda.domain.recommendation.constant;

import lombok.Getter;

@Getter
public enum LlmStatus {
    PENDING("대기중"), READY("완료"), FAILED("실패");

    private final String description;

    LlmStatus(String description) {
        this.description = description;
    }
}
