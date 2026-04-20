package com.generic4.itda.dto.proposal;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class AiInterviewGenerateRequest {

    private final ProposalForm proposalForm;
    private final String conversationText;
    private final String userMessage;

    @Builder(access = AccessLevel.PRIVATE)
    private AiInterviewGenerateRequest(
            ProposalForm proposalForm,
            String conversationText,
            String userMessage
    ) {
        Assert.notNull(proposalForm, "AI 인터뷰 제안서 폼은 필수값입니다.");
        Assert.hasText(userMessage, "AI 인터뷰 사용자 메시지는 필수값입니다.");

        this.proposalForm = proposalForm;
        this.conversationText = conversationText == null ? "" : conversationText;
        this.userMessage = userMessage.trim();
    }

    public static AiInterviewGenerateRequest of(
            ProposalForm proposalForm,
            String conversationText,
            String userMessage
    ) {
        return AiInterviewGenerateRequest.builder()
                .proposalForm(proposalForm)
                .conversationText(conversationText)
                .userMessage(userMessage)
                .build();
    }
}