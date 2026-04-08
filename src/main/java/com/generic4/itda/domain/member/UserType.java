package com.generic4.itda.domain.member;

import lombok.Getter;

@Getter
public enum UserType {
    CORPORATE("기업"), INDIVIDUAL("개인");

    private final String description;

    UserType(String description) {
        this.description = description;
    }
}
