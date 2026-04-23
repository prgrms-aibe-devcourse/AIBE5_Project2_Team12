package com.generic4.itda.dto.profile;

public enum ProfileContextType {
    RECOMMENDATION("추천 후보 검토"),
    MATCHING("매칭 상대 검토");

    private final String label;

    ProfileContextType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
