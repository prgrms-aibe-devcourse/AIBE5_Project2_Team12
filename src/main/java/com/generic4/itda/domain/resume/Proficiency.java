package com.generic4.itda.domain.resume;

import lombok.Getter;

@Getter
public enum Proficiency {
    BEGINNER("초급"), INTERMEDIATE("중급"), ADVANCED("고급");

    private final String description;

    Proficiency(String description) {
        this.description = description;
    }
}
