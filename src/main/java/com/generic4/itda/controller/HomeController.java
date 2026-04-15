package com.generic4.itda.controller;

import com.generic4.itda.dto.security.ItDaPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal ItDaPrincipal itDaPrincipal, Model model) {
        boolean authenticated = itDaPrincipal != null;

        model.addAttribute("authenticated", authenticated);

        if (!authenticated) {
            return "landing";
        }

        model.addAttribute("memberName", itDaPrincipal.getName());
        model.addAttribute("memberEmail", itDaPrincipal.getEmail());
        model.addAttribute("memberRole", itDaPrincipal.getRole().name());

        return "landing";
    }
}
