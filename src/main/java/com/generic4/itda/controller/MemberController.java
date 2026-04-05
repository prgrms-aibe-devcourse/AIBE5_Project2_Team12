package com.generic4.itda.controller;

import com.generic4.itda.dto.member.MemberSignUpForm;
import com.generic4.itda.exception.DuplicateEmailException;
import com.generic4.itda.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/signup")
    public String signupForm(@ModelAttribute("signUpForm") MemberSignUpForm signUpForm) {
        return "signup";
    }

    @PostMapping("/signup")
    public String signUp(@Valid @ModelAttribute("signUpForm") MemberSignUpForm signUpForm,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            memberService.signUp(signUpForm);
        } catch (DuplicateEmailException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            return "signup";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("signupFailed", ex.getMessage());
            return "signup";
        }

        return "redirect:/login?signupSuccess";
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(name = "error", defaultValue = "false") boolean error,
            @RequestParam(name = "signupSuccess", defaultValue = "false") boolean signupSuccess,
            Model model) {
        model.addAttribute("loginError", error);
        model.addAttribute("signupSuccess", signupSuccess);
        return "login";
    }
}
