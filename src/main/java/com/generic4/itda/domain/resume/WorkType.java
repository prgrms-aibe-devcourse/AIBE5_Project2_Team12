package com.generic4.itda.domain.resume;

import lombok.Getter;

@Getter
public enum WorkType {
    SITE("상주"), REMOTE("원격"), HYBRID("상주, 원격 모두 가능");

    private final String description;

    WorkType(String description) {
        this.description = description;
    }
}
