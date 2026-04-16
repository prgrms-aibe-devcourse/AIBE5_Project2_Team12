package com.generic4.itda.controller;

import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.resume.ResumeForm;
import com.generic4.itda.dto.resume.ResumeSkillForm;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.ResumeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal ItDaPrincipal principal, Model model) {
        try {
            resumeService.findByEmail(principal.getEmail());
            return "redirect:/resume/edit";
        } catch (IllegalStateException e) {
            // 이력서 없음 → 작성 페이지 진행
        }

        addCommonAttributes(model);
        model.addAttribute("resumeForm", new ResumeForm());
        model.addAttribute("resumeSkillForm", new ResumeSkillForm());
        return "resume/form";
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
            model.addAttribute("resumeSkillForm", new ResumeSkillForm());
            return "resume/form";
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
            model.addAttribute("resumeSkillForm", new ResumeSkillForm());
            return "resume/form";
        }

        return "redirect:/resume/edit";
    }

    @GetMapping("/edit")
    public String editForm(@AuthenticationPrincipal ItDaPrincipal principal, Model model) {
        Resume resume = resumeService.findByEmail(principal.getEmail());

        ResumeForm form = new ResumeForm();
        form.setIntroduction(resume.getIntroduction());
        form.setCareerYears(resume.getCareerYears());
        form.setCareer(resume.getCareer());
        form.setPreferredWorkType(resume.getPreferredWorkType());
        form.setPortfolioUrl(resume.getPortfolioUrl());
        form.setWritingStatus(resume.getWritingStatus());

        addCommonAttributes(model);
        model.addAttribute("resumeForm", form);
        model.addAttribute("resumeSkillForm", new ResumeSkillForm());
        model.addAttribute("resume", resume);
        return "resume/form";
    }

    @PostMapping("/edit")
    public String update(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("resumeForm") ResumeForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            Resume resume = resumeService.findByEmail(principal.getEmail());
            addCommonAttributes(model);
            model.addAttribute("resumeSkillForm", new ResumeSkillForm());
            model.addAttribute("resume", resume);
            return "resume/form";
        }

        resumeService.update(
                principal.getEmail(),
                form.getIntroduction(),
                form.getCareerYears(),
                form.getCareer(),
                form.getPreferredWorkType(),
                form.getWritingStatus(),
                form.getPortfolioUrl()
        );

        return "redirect:/resume/edit";
    }

    @PostMapping("/skill/add")
    public String addSkill(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("resumeSkillForm") ResumeSkillForm form,
            BindingResult bindingResult
    ) {
        if (!bindingResult.hasErrors()) {
            try {
                resumeService.addSkill(principal.getEmail(), form.getSkillId(), form.getProficiency());
            } catch (IllegalStateException e) {
                // 이미 등록된 스킬
            }
        }
        return "redirect:/resume/edit";
    }

    @PostMapping("/skill/update")
    public String updateSkill(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("resumeSkillForm") ResumeSkillForm form,
            BindingResult bindingResult
    ) {
        if (!bindingResult.hasErrors()) {
            resumeService.updateSkill(principal.getEmail(), form.getSkillId(), form.getProficiency());
        }
        return "redirect:/resume/edit";
    }

    @PostMapping("/skill/remove")
    public String removeSkill(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @ModelAttribute("resumeSkillForm") ResumeSkillForm form
    ) {
        resumeService.removeSkill(principal.getEmail(), form.getSkillId());
        return "redirect:/resume/edit";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("skills", resumeService.findAllSkills());
        model.addAttribute("workTypes", WorkType.values());
        model.addAttribute("proficiencies", Proficiency.values());
        model.addAttribute("writingStatuses", ResumeWritingStatus.values());
    }
}