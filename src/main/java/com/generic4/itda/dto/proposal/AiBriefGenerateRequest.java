package com.generic4.itda.dto.proposal;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class AiBriefGenerateRequest {

    private final String rawInputText;

    @Builder(access = AccessLevel.PRIVATE)
    private AiBriefGenerateRequest(String rawInputText) {
        Assert.hasText(rawInputText, "AI 브리프 원본 입력은 필수값입니다.");
        this.rawInputText = rawInputText;
    }

    public static AiBriefGenerateRequest from(String rawInputText) {
        return AiBriefGenerateRequest.builder()
                .rawInputText(rawInputText)
                .build();
    }
}
