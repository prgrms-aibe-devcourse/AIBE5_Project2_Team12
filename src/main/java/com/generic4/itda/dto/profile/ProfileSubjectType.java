package com.generic4.itda.dto.profile;

public enum ProfileSubjectType {
    FREELANCER("프리랜서"),
    CLIENT("클라이언트");

    private final String label;

    ProfileSubjectType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
