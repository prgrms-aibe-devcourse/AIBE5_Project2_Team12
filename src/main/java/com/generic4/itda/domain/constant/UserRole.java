package com.generic4.itda.domain.constant;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("관리자"), USER("일반 회원");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }
}
