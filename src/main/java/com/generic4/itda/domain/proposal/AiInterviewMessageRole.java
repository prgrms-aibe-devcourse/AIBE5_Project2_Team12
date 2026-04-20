package com.generic4.itda.domain.proposal;

import lombok.Getter;

@Getter
public enum AiInterviewMessageRole {

    USER("사용자"),
    ASSISTANT("AI");

    private final String description;

    AiInterviewMessageRole(String description) {
        this.description = description;
    }
}