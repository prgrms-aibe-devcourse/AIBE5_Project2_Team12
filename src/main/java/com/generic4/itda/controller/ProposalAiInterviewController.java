package com.generic4.itda.controller;

import com.generic4.itda.dto.proposal.AiInterviewDraftSaveRequest;
import com.generic4.itda.dto.proposal.AiInterviewSendMessageRequest;
import com.generic4.itda.dto.proposal.AiInterviewSendMessageResponse;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.ProposalAiInterviewService;
import com.generic4.itda.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/proposals")
public class ProposalAiInterviewController {

    private final ProposalAiInterviewService proposalAiInterviewService;
    private final ProposalService proposalService;

    @PatchMapping("/{proposalId}/ai-interview/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveDraft(
            @AuthenticationPrincipal ItDaPrincipal itDaPrincipal,
            @PathVariable Long proposalId,
            @Valid @RequestBody AiInterviewDraftSaveRequest request
    ) {
        proposalService.saveInterviewDraft(
                proposalId,
                itDaPrincipal.getEmail(),
                request
        );
    }

    @PostMapping("/{proposalId}/ai-interview/messages")
    public AiInterviewSendMessageResponse sendMessage(
            @AuthenticationPrincipal ItDaPrincipal itDaPrincipal,
            @PathVariable Long proposalId,
            @Valid @RequestBody AiInterviewSendMessageRequest request
    ) {
        return proposalAiInterviewService.sendMessage(
                proposalId,
                itDaPrincipal.getEmail(),
                request.getMessage()
        );
    }
}