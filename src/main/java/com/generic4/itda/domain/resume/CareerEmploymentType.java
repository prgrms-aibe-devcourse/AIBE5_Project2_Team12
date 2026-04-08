package com.generic4.itda.domain.resume;

import lombok.Getter;

@Getter
public enum CareerEmploymentType {
    FULL_TIME("정규직"),
    CONTRACT("계약직"),
    INTERN("인턴"),
    FREELANCE("프리랜서"),
    PART_TIME("파트타임"),
    ETC("기타");

    private final String description;

    CareerEmploymentType(String description) {
        this.description = description;
    }
}
