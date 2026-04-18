package com.generic4.itda.domain.matching.constant;

import lombok.Getter;

@Getter
public enum MatchingStatus {
    PROPOSED("제안됨"),
    ACCEPTED("수락됨"),
    REJECTED("거절됨"),
    IN_PROGRESS("협의 중"),
    COMPLETED("완료됨"),
    CANCELED("취소됨");

    private final String description;

    MatchingStatus(String description) {
        this.description = description;
    }
}

