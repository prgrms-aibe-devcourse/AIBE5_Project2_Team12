package com.generic4.itda.domain.resume;

import lombok.Getter;

@Getter
public enum Proficiency {
    BEGINNER("초급", 1), INTERMEDIATE("중급", 2), ADVANCED("고급", 3);

    private final String description;
    private final int priority;

    Proficiency(String description, int priority) {
        this.description = description;
        this.priority = priority;
    }
}
