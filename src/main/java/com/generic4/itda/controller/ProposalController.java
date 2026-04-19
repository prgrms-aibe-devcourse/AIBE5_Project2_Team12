package com.generic4.itda.controller;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.dto.proposal.ProposalPositionForm;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.service.ProposalService;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private static final String SAVE_ACTION = "save";
    private static final String REGISTER_ACTION = "register";

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
            @RequestParam(name = "submitAction", defaultValue = SAVE_ACTION) String submitAction,
            Model model
    ) {
        boolean registerAction = isRegisterAction(submitAction);
        sanitizeForSubmit(form);
        validateForSubmit(form, bindingResult, registerAction);

        if (bindingResult.hasErrors()) {
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("proposalId", null);
            model.addAttribute("isNew", true);
            return "client/proposalForm";
        }

        try {
            Proposal proposal = registerAction
                    ? proposalService.register(principal.getEmail(), form)
                    : proposalService.saveDraft(principal.getEmail(), form);

            return registerAction
                    ? "redirect:/proposals/" + proposal.getId() + "/recommendations"
                    : "redirect:/proposals/" + proposal.getId() + "/edit";
        } catch (IllegalArgumentException | IllegalStateException e) {
            rejectGlobal(bindingResult, e.getMessage());
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("proposalId", null);
            model.addAttribute("isNew", true);
            return "client/proposalForm";
        }
    }

    @GetMapping("/{proposalId}/edit")
    public String editForm(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Proposal proposal = proposalService.findOwnedProposal(proposalId, principal.getEmail());

            if (proposal.getStatus() == ProposalStatus.MATCHING) {
                proposalService.validateCanCreateEditDraft(proposalId, principal.getEmail());
                addMemberAttributes(model, principal);
                model.addAttribute("proposal", proposal);
                model.addAttribute("proposalId", proposalId);
                return "client/proposalEditStart";
            }

            if (proposal.getStatus() == ProposalStatus.COMPLETE) {
                redirectAttributes.addFlashAttribute("errorMessage", "종료된 제안서는 수정할 수 없습니다.");
                return "redirect:/client/dashboard";
            }

            ProposalForm form = ProposalForm.from(proposal);

            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("proposalForm", form);
            model.addAttribute("proposalId", proposalId);
            model.addAttribute("isNew", false);

            return "client/proposalForm";
        } catch (ProposalNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 제안서입니다.");
            return "redirect:/client/dashboard";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인 제안서만 조회하거나 수정할 수 있습니다.");
            return "redirect:/client/dashboard";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/client/dashboard";
        }
    }

    @PostMapping("/{proposalId}/edit-draft")
    public String createEditDraft(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Proposal proposal = proposalService.createEditDraft(proposalId, principal.getEmail());
            if (!proposal.getId().equals(proposalId)) {
                redirectAttributes.addFlashAttribute("noticeMessage", "기존 매칭 이력을 보존하기 위해 새 초안으로 이동했습니다.");
            }
            return "redirect:/proposals/" + proposal.getId() + "/edit";
        } catch (ProposalNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 제안서입니다.");
            return "redirect:/client/dashboard";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인 제안서만 조회하거나 수정할 수 있습니다.");
            return "redirect:/client/dashboard";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/client/dashboard";
        }
    }

    @PostMapping("/{proposalId}/edit")
    public String update(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("proposalForm") ProposalForm form,
            BindingResult bindingResult,
            @RequestParam(name = "submitAction", defaultValue = SAVE_ACTION) String submitAction,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        boolean registerAction = isRegisterAction(submitAction);
        sanitizeForSubmit(form);
        validateForSubmit(form, bindingResult, registerAction);

        if (bindingResult.hasErrors()) {
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("proposalId", proposalId);
            model.addAttribute("isNew", false);
            return "client/proposalForm";
        }

        try {
            Proposal proposal = registerAction
                    ? proposalService.register(proposalId, principal.getEmail(), form)
                    : proposalService.saveDraft(proposalId, principal.getEmail(), form);

            return registerAction
                    ? "redirect:/proposals/" + proposal.getId() + "/recommendations"
                    : "redirect:/proposals/" + proposal.getId() + "/edit";
        } catch (ProposalNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 제안서입니다.");
            return "redirect:/client/dashboard";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인 제안서만 조회하거나 수정할 수 있습니다.");
            return "redirect:/client/dashboard";
        } catch (IllegalArgumentException | IllegalStateException e) {
            rejectGlobal(bindingResult, e.getMessage());
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("proposalId", proposalId);
            model.addAttribute("isNew", false);
            return "client/proposalForm";
        }
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("workTypes", ProposalWorkType.values());
        model.addAttribute("positionOptions", positionRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
    }

    private void addMemberAttributes(Model model, ItDaPrincipal principal) {
        model.addAttribute("memberName", principal.getName());
        model.addAttribute("memberEmail", principal.getEmail());
    }

    private boolean isRegisterAction(String submitAction) {
        return REGISTER_ACTION.equalsIgnoreCase(submitAction);
    }

    private void sanitizeForSubmit(ProposalForm form) {
        if (form.getRawInputText() == null) {
            form.setRawInputText("");
        }

        if (form.getPositions() == null) {
            return;
        }

        for (ProposalPositionForm position : form.getPositions()) {
            if (position.getWorkType() == ProposalWorkType.REMOTE) {
                position.setWorkPlace(null);
            }
        }
    }

    private void validateForSubmit(ProposalForm form, BindingResult bindingResult, boolean registerAction) {
        validatePositions(form, bindingResult);
        if (registerAction) {
            validateRegisterRequirements(form, bindingResult);
        }
    }

    private void validatePositions(ProposalForm form, BindingResult bindingResult) {
        List<ProposalPositionForm> positions = form.getPositions();
        if (positions == null || positions.isEmpty()) {
            return;
        }

        for (int index = 0; index < positions.size(); index++) {
            ProposalPositionForm position = positions.get(index);
            String label = StringUtils.hasText(position.getTitle())
                    ? "'" + position.getTitle().trim() + "' 포지션"
                    : "포지션 " + (index + 1);

            if (position.getPositionId() == null) {
                rejectGlobal(bindingResult, label + "의 직무를 선택해야 합니다.");
            }
            if (!StringUtils.hasText(position.getTitle())) {
                rejectGlobal(bindingResult, label + "의 포지션 제목을 입력해야 합니다.");
            }
            if (position.getWorkType() == null) {
                rejectGlobal(bindingResult, label + "의 근무 형태를 선택해야 합니다.");
            }
            if (position.getHeadCount() == null || position.getHeadCount() < 1) {
                rejectGlobal(bindingResult, label + "의 모집 인원은 1명 이상이어야 합니다.");
            }
            if (position.getWorkType() == ProposalWorkType.SITE || position.getWorkType() == ProposalWorkType.HYBRID) {
                if (!StringUtils.hasText(position.getWorkPlace())) {
                    rejectGlobal(bindingResult, label + "은(는) 상주 또는 하이브리드 근무이므로 근무지를 입력해야 합니다.");
                }
            }
            if (position.getWorkType() == ProposalWorkType.REMOTE && StringUtils.hasText(position.getWorkPlace())) {
                rejectGlobal(bindingResult, label + "은(는) 원격 근무이므로 근무지를 입력할 수 없습니다.");
            }
            if (position.getUnitBudgetMin() != null && position.getUnitBudgetMax() != null
                    && position.getUnitBudgetMin() > position.getUnitBudgetMax()) {
                rejectGlobal(bindingResult, label + "의 최소 예산은 최대 예산보다 클 수 없습니다.");
            }
            if (position.getCareerMinYears() != null && position.getCareerMaxYears() != null
                    && position.getCareerMinYears() > position.getCareerMaxYears()) {
                rejectGlobal(bindingResult, label + "의 최소 경력은 최대 경력보다 클 수 없습니다.");
            }
            if (form.getExpectedPeriod() != null && position.getExpectedPeriod() != null
                    && position.getExpectedPeriod() > form.getExpectedPeriod()) {
                rejectGlobal(bindingResult, label + "의 예상 기간은 제안서 전체 예상 기간보다 길 수 없습니다.");
            }
            if (hasSkillOverlap(position)) {
                rejectGlobal(bindingResult, label + "의 필수 기술과 우대 기술에는 같은 값을 넣을 수 없습니다.");
            }
        }
    }

    private void validateRegisterRequirements(ProposalForm form, BindingResult bindingResult) {
        List<ProposalPositionForm> positions = form.getPositions();
        if (positions == null || positions.isEmpty()) {
            rejectGlobal(bindingResult, "제안서 등록을 위해서는 포지션이 1개 이상 필요합니다.");
            return;
        }

        if (form.getExpectedPeriod() == null) {
            bindingResult.rejectValue("expectedPeriod", "proposal.expectedPeriod.required",
                    "제안서 등록을 위해서는 전체 예상 기간을 입력해야 합니다.");
        }

        if (proposalService.calculateTotalBudgetMin(form) == null || proposalService.calculateTotalBudgetMax(form) == null) {
            rejectGlobal(bindingResult, "제안서 등록을 위해서는 모든 포지션의 인원과 최소/최대 예산이 입력되어야 합니다.");
        }

        for (int index = 0; index < positions.size(); index++) {
            ProposalPositionForm position = positions.get(index);
            String label = StringUtils.hasText(position.getTitle())
                    ? "'" + position.getTitle().trim() + "' 포지션"
                    : "포지션 " + (index + 1);

            if (position.getUnitBudgetMin() == null || position.getUnitBudgetMax() == null) {
                rejectGlobal(bindingResult, label + "은(는) 제안서 등록 전에 최소/최대 예산을 모두 입력해야 합니다.");
            }
            if (position.getExpectedPeriod() == null) {
                rejectGlobal(bindingResult, label + "은(는) 제안서 등록 전에 예상 기간을 입력해야 합니다.");
            }
            if (!hasAnySkill(position)) {
                rejectGlobal(bindingResult, label + "은(는) 필수 기술 또는 우대 기술 중 하나 이상이 필요합니다.");
            }
        }
    }

    private boolean hasAnySkill(ProposalPositionForm position) {
        return hasAnyText(position.getEssentialSkillNames()) || hasAnyText(position.getPreferredSkillNames());
    }

    private boolean hasAnyText(List<String> values) {
        return values != null && values.stream().anyMatch(StringUtils::hasText);
    }

    private boolean hasSkillOverlap(ProposalPositionForm position) {
        Set<String> essential = normalizedSkillSet(position.getEssentialSkillNames());
        Set<String> preferred = normalizedSkillSet(position.getPreferredSkillNames());
        essential.retainAll(preferred);
        return !essential.isEmpty();
    }

    private Set<String> normalizedSkillSet(List<String> skills) {
        Set<String> normalized = new HashSet<>();
        if (skills == null) {
            return normalized;
        }
        for (String skill : skills) {
            if (StringUtils.hasText(skill)) {
                normalized.add(skill.trim());
            }
        }
        return normalized;
    }

    private void rejectGlobal(BindingResult bindingResult, String message) {
        boolean exists = bindingResult.getGlobalErrors().stream()
                .anyMatch(error -> message.equals(error.getDefaultMessage()));
        if (!exists) {
            bindingResult.reject("proposalForm.invalid", message);
        }
    }
}
