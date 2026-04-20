package com.generic4.itda.dto.proposal;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AiInterviewSendMessageResponse {

    private final ProposalForm proposalForm;
    private final List<AiInterviewMessageResponse> messages;
    private final String assistantMessage;

    @Builder(access = AccessLevel.PRIVATE)
    private AiInterviewSendMessageResponse(
            ProposalForm proposalForm,
            List<AiInterviewMessageResponse> messages,
            String assistantMessage
    ) {
        this.proposalForm = proposalForm;
        this.messages = messages == null ? List.of() : List.copyOf(messages);
        this.assistantMessage = assistantMessage;
    }

    public static AiInterviewSendMessageResponse of(
            ProposalForm proposalForm,
            List<AiInterviewMessageResponse> messages,
            String assistantMessage
    ) {
        return AiInterviewSendMessageResponse.builder()
                .proposalForm(proposalForm)
                .messages(messages)
                .assistantMessage(assistantMessage)
                .build();
    }
}