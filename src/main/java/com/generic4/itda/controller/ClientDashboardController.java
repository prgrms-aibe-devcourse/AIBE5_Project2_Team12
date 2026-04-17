package com.generic4.itda.controller;

import com.generic4.itda.dto.client.ClientDashboardFilter;
import com.generic4.itda.dto.client.ClientDashboardViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.ClientDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ClientDashboardController {

    private final ClientDashboardService clientDashboardService;

    @GetMapping("/client/dashboard")
    public String dashboard(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @RequestParam(name = "filter", required = false) String filter,
            Model model
    ) {
        ClientDashboardFilter selectedFilter = ClientDashboardFilter.from(filter);
        ClientDashboardViewModel dashboard = clientDashboardService.getDashboard(principal.getEmail(), selectedFilter);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("memberName", principal.getName());
        model.addAttribute("memberEmail", principal.getEmail());

        return "client/dashboard";
    }
}
