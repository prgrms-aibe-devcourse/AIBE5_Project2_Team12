package com.generic4.itda.dto.proposal;

import com.generic4.itda.domain.proposal.ProposalAiInterviewMessage;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AiInterviewMessageResponse {

    private final Long id;
    private final String role;
    private final String content;
    private final Integer sequence;

    @Builder(access = AccessLevel.PRIVATE)
    private AiInterviewMessageResponse(Long id, String role, String content, Integer sequence) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.sequence = sequence;
    }

    public static AiInterviewMessageResponse from(ProposalAiInterviewMessage message) {
        return AiInterviewMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .sequence(message.getSequence())
                .build();
    }
}