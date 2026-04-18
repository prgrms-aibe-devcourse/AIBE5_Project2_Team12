package com.generic4.itda.controller;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;
    private final PositionRepository positionRepository;

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal ItDaPrincipal principal, Model model) {
        ProposalForm form = ProposalForm.createDefault();

        addCommonAttributes(model);
        addMemberAttributes(model, principal);
        model.addAttribute("proposalForm", form);
        model.addAttribute("proposalId", null);
        model.addAttribute("isNew", true);

        return "client/proposalForm";
    }

    @PostMapping("/new")
    public String create(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("proposalForm") ProposalForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("proposalId", null);
            model.addAttribute("isNew", true);
            return "client/proposalForm";
        }

        Proposal proposal = proposalService.create(principal.getEmail(), form);
        return "redirect:/proposals/" + proposal.getId() + "/edit";
    }

    @GetMapping("/{proposalId}/edit")
    public String editForm(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        Proposal proposal = proposalService.findOwnedProposal(proposalId, principal.getEmail());
        ProposalForm form = ProposalForm.from(proposal);

        addCommonAttributes(model);
        addMemberAttributes(model, principal);
        model.addAttribute("proposalForm", form);
        model.addAttribute("proposalId", proposalId);
        model.addAttribute("isNew", false);

        return "client/proposalForm";
    }

    @PostMapping("/{proposalId}/edit")
    public String update(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("proposalForm") ProposalForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("proposalId", proposalId);
            model.addAttribute("isNew", false);
            return "client/proposalForm";
        }

        proposalService.update(proposalId, principal.getEmail(), form);
        return "redirect:/proposals/" + proposalId + "/edit";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("workTypes", ProposalWorkType.values());
        model.addAttribute("positionOptions", positionRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
    }

    private void addMemberAttributes(Model model, ItDaPrincipal principal) {
        model.addAttribute("memberName", principal.getName());
        model.addAttribute("memberEmail", principal.getEmail());
    }
}
