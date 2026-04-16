package com.generic4.itda.controller;

import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.dto.resume.ResumeForm;
import com.generic4.itda.dto.resume.ResumeSkillForm;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping("")
    public String index(@AuthenticationPrincipal ItDaPrincipal principal) {
        try {
            resumeService.findByEmail(principal.getEmail());
            return "redirect:/resume/edit";
        } catch (IllegalStateException ignored) {
            return "redirect:/resume/new";
        }
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal ItDaPrincipal principal, Model model) {
        try {
            resumeService.findByEmail(principal.getEmail());
            return "redirect:/resume/edit";
        } catch (IllegalStateException ignored) {}

        ResumeForm form = new ResumeForm();
        form.setCareerYears((byte) 0);
        form.setWritingStatus(ResumeWritingStatus.WRITING);
        form.setIntroduction("");
        form.setPubliclyVisible(true);
        form.setAiMatchingEnabled(true);

        addCommonAttributes(model);
        addMemberAttributes(model, principal);
        model.addAttribute("resumeForm", form);
        model.addAttribute("resumeSkillForm", new ResumeSkillForm());
        model.addAttribute("isNew", true);

        return "freelancer/resumeForm";
    }

    @PostMapping("/new")
    public String create(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("resumeForm") ResumeForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("resumeSkillForm", new ResumeSkillForm());
            model.addAttribute("isNew", true);
            model.addAttribute("editing", true);
            return "freelancer/resumeForm";
        }

        try {
            resumeService.create(
                    principal.getEmail(),
                    form.getIntroduction(),
                    form.getCareerYears(),
                    form.getCareer(),
                    form.getPreferredWorkType(),
                    form.getWritingStatus(),
                    form.getPortfolioUrl()
            );
        } catch (IllegalStateException e) {
            bindingResult.reject("resumeExists", e.getMessage());
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("resumeSkillForm", new ResumeSkillForm());
            model.addAttribute("isNew", true);
            model.addAttribute("editing", true);
            return "freelancer/resumeForm";
        }

        return "redirect:/resume/edit";
    }

    @GetMapping("/edit")
    public String editForm(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @RequestParam(value = "mode", required = false) String mode,
            Model model
    ) {
        Resume resume = resumeService.findByEmail(principal.getEmail());

        // DTO 변환 로직을 하단 전용 메서드로 호출
        ResumeForm form = convertToForm(resume);

        addCommonAttributes(model);
        addMemberAttributes(model, principal);

        // mode=modify가 있으면 수정 가능(true), 없으면 조회 전용(false)
        boolean isEditable = "modify".equals(mode);

        model.addAttribute("resumeForm", form);
        model.addAttribute("resumeSkillForm", new ResumeSkillForm());
        model.addAttribute("resume", resume);
        model.addAttribute("isNew", false);
        model.addAttribute("editable", isEditable);

        return "freelancer/resumeForm";
    }

    @PostMapping("/edit")
    public String update(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("resumeForm") ResumeForm form,
            BindingResult bindingResult,
            Model model
    ) {
        // 1. 유효성 검사 실패 시
        if (bindingResult.hasErrors()) {
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("isNew", false);
            model.addAttribute("editable", true); // 에러가 났으니 다시 수정할 수 있게 true 유지
            return "freelancer/resumeForm";
        }

        try {
            resumeService.update(
                    principal.getEmail(),
                    form.getIntroduction(),
                    form.getCareerYears(),
                    form.getCareer(),
                    form.getPreferredWorkType(),
                    form.getWritingStatus(),
                    form.getPortfolioUrl(),
                    form.isPubliclyVisible(),
                    form.isAiMatchingEnabled()
            );
        } catch (IllegalStateException e) {
            return "redirect:/resume/new";
        }

        // 2. 저장 완료 후: 파라미터 없이 리다이렉트 -> mode가 없으므로 다시 '조회 모드(readonly)'가 됨
        return "redirect:/resume/edit";
    }

    // 중복 코드 방지를 위한 변환 메서드
    private ResumeForm convertToForm(Resume resume) {
        ResumeForm form = new ResumeForm();
        form.setIntroduction(resume.getIntroduction());
        form.setCareerYears(resume.getCareerYears());
        form.setCareer(resume.getCareer());
        form.setPreferredWorkType(resume.getPreferredWorkType());
        form.setPortfolioUrl(resume.getPortfolioUrl());
        form.setWritingStatus(resume.getWritingStatus());
        form.setPubliclyVisible(resume.isPubliclyVisible());
        form.setAiMatchingEnabled(resume.isAiMatchingEnabled());
        return form;
    }

    @PostMapping("/skill/add")
    public String addSkill(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("resumeSkillForm") ResumeSkillForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return renderEditPageForSkillForm(principal, model, true);
        }

        try {
            resumeService.addSkill(principal.getEmail(), form.getSkillId(), form.getProficiency());
        } catch (IllegalStateException e) {
            bindingResult.reject("skillAddFailed", e.getMessage());
            return renderEditPageForSkillForm(principal, model, true);
        }

        return "redirect:/resume/edit?mode=modify";
    }

    @PostMapping("/skill/update")
    public String updateSkill(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("resumeSkillForm") ResumeSkillForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return renderEditPageForSkillForm(principal, model, true);
        }

        try {
            resumeService.updateSkill(principal.getEmail(), form.getSkillId(), form.getProficiency());
        } catch (IllegalStateException e) {
            bindingResult.reject("skillUpdateFailed", e.getMessage());
            return renderEditPageForSkillForm(principal, model, true);
        }

        return "redirect:/resume/edit?mode=modify";
    }

    @PostMapping("/skill/remove")
    public String removeSkill(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @ModelAttribute("resumeSkillForm") ResumeSkillForm form
    ) {
        if (form.getSkillId() == null) {
            return "redirect:/resume/edit";
        }

        resumeService.removeSkill(principal.getEmail(), form.getSkillId());
        return "redirect:/resume/edit?mode=modify";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("skills", resumeService.findAllSkills());
        model.addAttribute("workTypes", WorkType.values());
        model.addAttribute("proficiencies", Proficiency.values());
        model.addAttribute("writingStatuses", ResumeWritingStatus.values());
    }

    private void addMemberAttributes(Model model, ItDaPrincipal principal) {
        model.addAttribute("memberName", principal.getName());
        model.addAttribute("memberEmail", principal.getEmail());
        model.addAttribute("memberPhone", principal.getPhone());
    }

    private String renderEditPageForSkillForm(ItDaPrincipal principal, Model model, boolean editing) {
        Resume resume;
        try {
            resume = resumeService.findByEmail(principal.getEmail());
        } catch (IllegalStateException e) {
            return "redirect:/resume/new";
        }

        ResumeForm form = new ResumeForm();
        form.setIntroduction(resume.getIntroduction());
        form.setCareerYears(resume.getCareerYears());
        form.setCareer(resume.getCareer());
        form.setPreferredWorkType(resume.getPreferredWorkType());
        form.setPortfolioUrl(resume.getPortfolioUrl());
        form.setWritingStatus(resume.getWritingStatus());
        form.setPubliclyVisible(resume.isPubliclyVisible());
        form.setAiMatchingEnabled(resume.isAiMatchingEnabled());

        addCommonAttributes(model);
        addMemberAttributes(model, principal);
        model.addAttribute("resumeForm", form);
        model.addAttribute("resume", resume);
        model.addAttribute("isNew", false);
        model.addAttribute("editable", editing);
        return "freelancer/resumeForm";
    }
}
