package com.generic4.itda.controller;

import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.service.ProposalAiBriefService;
import lombok.RequiredArgsConstructor;
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
    public AiBriefResult generate(@PathVariable Long proposalId) {
        return proposalAiBriefService.generate(proposalId);
    }
}
