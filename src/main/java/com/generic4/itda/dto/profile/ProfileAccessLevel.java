package com.generic4.itda.dto.profile;

public enum ProfileAccessLevel {
    PREVIEW("미리보기", "추천/요청 판단에 필요한 정보만 공개됩니다."),
    FULL("전체 공개", "매칭 성립 후 협업에 필요한 정보까지 공개됩니다.");

    private final String label;
    private final String guideMessage;

    ProfileAccessLevel(String label, String guideMessage) {
        this.label = label;
        this.guideMessage = guideMessage;
    }

    public String label() {
        return label;
    }

    public String guideMessage() {
        return guideMessage;
    }
}
