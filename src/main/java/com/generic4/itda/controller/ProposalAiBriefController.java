package com.generic4.itda.controller;

import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.ProposalAiBriefService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/proposals")
public class ProposalAiBriefController {

    private final ProposalAiBriefService proposalAiBriefService;

    @PostMapping("/{proposalId}/ai-brief")
    public AiBriefResult generate(@AuthenticationPrincipal ItDaPrincipal itDaPrincipal,
            @PathVariable Long proposalId) {
        return proposalAiBriefService.generate(proposalId, itDaPrincipal.getEmail());
    }
}
