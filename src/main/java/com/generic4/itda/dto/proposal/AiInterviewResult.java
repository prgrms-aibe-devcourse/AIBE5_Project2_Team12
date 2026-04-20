package com.generic4.itda.dto.proposal;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class AiInterviewResult {

    private final AiBriefResult aiBriefResult;
    private final String assistantMessage;

    @Builder(access = AccessLevel.PRIVATE)
    private AiInterviewResult(AiBriefResult aiBriefResult, String assistantMessage) {
        Assert.notNull(aiBriefResult, "AI 인터뷰 브리프 결과는 필수값입니다.");
        Assert.hasText(assistantMessage, "AI 인터뷰 후속 질문은 필수값입니다.");

        this.aiBriefResult = aiBriefResult;
        this.assistantMessage = assistantMessage.trim();
    }

    public static AiInterviewResult of(AiBriefResult aiBriefResult, String assistantMessage) {
        return AiInterviewResult.builder()
                .aiBriefResult(aiBriefResult)
                .assistantMessage(assistantMessage)
                .build();
    }
}