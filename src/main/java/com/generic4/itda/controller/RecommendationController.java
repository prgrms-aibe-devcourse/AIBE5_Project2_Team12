package com.generic4.itda.controller;

import com.generic4.itda.dto.recommend.RecommendationEntryViewModel;
import com.generic4.itda.dto.recommend.RecommendationRequestForm;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.recommend.RecommendationEntryService;
import com.generic4.itda.service.recommend.RecommendationRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationEntryService recommendationEntryService;
    private final RecommendationRunService recommendationRunService;

    @GetMapping("/proposals/{proposalId}/recommendations")
    public String entry(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        RecommendationEntryViewModel entry = recommendationEntryService.getEntry(proposalId, principal.getEmail());

        model.addAttribute("entry", entry);
        model.addAttribute("requestForm", new RecommendationRequestForm(entry.selectedProposalPositionId()));

        return "recommendation/entry";
    }
}
