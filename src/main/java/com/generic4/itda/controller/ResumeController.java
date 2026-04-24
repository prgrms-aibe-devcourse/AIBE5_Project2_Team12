package com.generic4.itda.controller;

import com.generic4.itda.domain.resume.CareerEmploymentType;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.dto.resume.CareerItemForm;
import com.generic4.itda.dto.resume.ResumeForm;
import com.generic4.itda.dto.resume.ResumeSkillForm;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping("")
    public String index(@AuthenticationPrincipal ItDaPrincipal principal) {
        try {
            resumeService.findByEmail(principal.getEmail());
            return "redirect:/resumes/me";
        } catch (IllegalStateException ignored) {
            return "redirect:/resumes/new";
        }
    }

    @GetMapping("/me")
    public String viewFrom (@AuthenticationPrincipal ItDaPrincipal principal, Model model){
        Resume resume;
        try {
            resume = resumeService.findByEmail(principal.getEmail());
        } catch (IllegalStateException ignored) {
            return "redirect:/resumes/new";
        }
        ResumeForm form = ResumeForm.from(resume);

        addCommonAttributes(model);
        addMemberAttributes(model, principal);

        model.addAttribute("resumeForm", form);
        model.addAttribute("resume", resume);
        model.addAttribute("isNew", false);
        model.addAttribute("editable", false);

        return "freelancer/resumeForm";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal ItDaPrincipal principal, Model model) {
        try {
            resumeService.findByEmail(principal.getEmail());
            return "redirect:/resumes/me";
        } catch (IllegalStateException ignored) {
        }

        ResumeForm form = ResumeForm.createDefault();

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
            return "freelancer/resumeForm";
        }

        try {
            resumeService.create(principal.getEmail(), form);
        } catch (IllegalStateException e) {
            bindingResult.reject("resumeExists", e.getMessage());
            addCommonAttributes(model);
            addMemberAttributes(model, principal);
            model.addAttribute("resumeSkillForm", new ResumeSkillForm());
            model.addAttribute("isNew", true);
            return "freelancer/resumeForm";
        }

        return "redirect:/resumes/me";
    }

    @GetMapping("/edit")
    public String editForm(@AuthenticationPrincipal ItDaPrincipal principal, Model model) {
        Resume resume;
        try {
            resume = resumeService.findByEmail(principal.getEmail());
        } catch (IllegalStateException ignored) {
            return "redirect:/resumes/new";
        }
        ResumeForm form = ResumeForm.from(resume);

        addCommonAttributes(model);
        addMemberAttributes(model, principal);

        model.addAttribute("resumeForm", form);
        model.addAttribute("resumeSkillForm", new ResumeSkillForm());
        model.addAttribute("resume", resume);
        model.addAttribute("isNew", false);
        model.addAttribute("editable", true);

        return "freelancer/resumeForm";
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
            addMemberAttributes(model, principal);
            model.addAttribute("resumeSkillForm", new ResumeSkillForm());
            model.addAttribute("resumeForm", form);
            model.addAttribute("resume", resume);
            model.addAttribute("isNew", false);
            model.addAttribute("editable", true);
            return "freelancer/resumeForm";
        }

        try {
            resumeService.update(principal.getEmail(), form);
        } catch (IllegalStateException e) {
            return "redirect:/resumes/new";
        }

        return "redirect:/resumes/me";
    }

    @PostMapping("/career/add")
    public String addCareer(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute CareerItemForm form,
            Model model
    ) {
        resumeService.addCareer(principal.getEmail(), form.toPayload());
        return prepareCareerSection(principal, model);
    }

    @PostMapping("/career/update")
    public String updateCareer(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute CareerItemForm form,
            Model model
    ) {
        resumeService.updateCareer(principal.getEmail(), form.getIndex(), form.toPayload());
        return prepareCareerSection(principal, model);
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleCareerValidation(org.springframework.validation.BindException ex) {
        Map<String, String> errors = ex.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage(),
                        (a, b) -> a
                ));
        return ResponseEntity.badRequest().body(errors);
    }

    @PostMapping("/career/remove")
    public String removeCareer(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @RequestParam int index,
            Model model
    ) {
        resumeService.removeCareer(principal.getEmail(), index);
        return prepareCareerSection(principal, model);
    }

    private String prepareCareerSection(ItDaPrincipal principal, Model model) {
        Resume resume = resumeService.findByEmail(principal.getEmail());
        addCommonAttributes(model);
        model.addAttribute("resume", resume);
        model.addAttribute("isEditing", true);
        model.addAttribute("isNew", false);
        return "freelancer/resumeForm :: #careerSection";
    }

    @PostMapping("/skill/add")
    public String addSkill(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @Valid @ModelAttribute("resumeSkillForm") ResumeSkillForm form,
            BindingResult bindingResult,
            Model model
    ) {
        // 1. 검증 오류가 없을 때만 저장 로직 실행
        if (!bindingResult.hasErrors()) {
            try {
                resumeService.addSkill(principal.getEmail(), form.getSkillId(), form.getProficiency());
            } catch (IllegalStateException e) {
                // 중복 스킬 등 비즈니스 예외 처리
                bindingResult.rejectValue("skillId", "duplicate", e.getMessage());
            }
        }

        // 2. 조각을 다시 그리기 위한 데이터 준비 (핵심)
        Resume resume = resumeService.findByEmail(principal.getEmail());
        addCommonAttributes(model); // skills, proficiencies 목록 담기
        model.addAttribute("resume", resume);
        model.addAttribute("isEditing", true);
        // 에러가 있다면 입력하던 폼을 그대로, 없다면 빈 폼을 전달
        model.addAttribute("resumeSkillForm", bindingResult.hasErrors() ? form : new ResumeSkillForm());

        // 3. 파일명 :: #ID 반환 (이 부분을 본인의 파일명에 맞게 수정하세요)
        return "freelancer/resumeForm :: #skillSection";
    }

    @PostMapping("/skill/remove")
    public String removeSkill(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @ModelAttribute("resumeSkillForm") ResumeSkillForm form,
            Model model
    ) {
        if (form.getSkillId() != null) {
            resumeService.removeSkill(principal.getEmail(), form.getSkillId());
        }

        // 삭제 후에도 목록을 갱신해서 조각 반환
        Resume resume = resumeService.findByEmail(principal.getEmail());
        addCommonAttributes(model);
        model.addAttribute("resume", resume);
        model.addAttribute("isEditing", true);
        model.addAttribute("resumeSkillForm", new ResumeSkillForm());

        return "freelancer/resumeForm :: #skillSection";
    }
    @PostMapping("/skill/update")
    public String updateSkill(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @ModelAttribute("resumeSkillForm") ResumeSkillForm form,
            Model model
    ) {
        // 특정 스킬의 숙련도 업데이트
        resumeService.updateSkill(principal.getEmail(), form.getSkillId(), form.getProficiency());

        // 갱신된 데이터로 조각 반환
        Resume resume = resumeService.findByEmail(principal.getEmail());
        addCommonAttributes(model);
        model.addAttribute("resume", resume);
        model.addAttribute("isEditing", true);
        model.addAttribute("resumeSkillForm", new ResumeSkillForm());

        return "freelancer/resumeForm :: #skillSection";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("skills", resumeService.findAllSkills());
        model.addAttribute("workTypes", WorkType.values());
        model.addAttribute("proficiencies", Proficiency.values());
        model.addAttribute("writingStatuses", ResumeWritingStatus.values());
        model.addAttribute("employmentTypes", CareerEmploymentType.values());
    }

    private void addMemberAttributes(Model model, ItDaPrincipal principal) {
        model.addAttribute("memberName", principal.getName());
        model.addAttribute("memberEmail", principal.getEmail());
        model.addAttribute("memberPhone", principal.getPhone());
    }

    private String renderEditPageForSkillForm(ItDaPrincipal principal, Model model, boolean editable) {
        Resume resume = resumeService.findByEmail(principal.getEmail());
        ResumeForm form = ResumeForm.from(resume);

        addCommonAttributes(model);
        addMemberAttributes(model, principal);
        model.addAttribute("resumeForm", form);
        model.addAttribute("resume", resume);
        model.addAttribute("isNew", false);
        model.addAttribute("editable", editable);
        return "freelancer/resumeForm";
    }
}
