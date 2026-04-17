package com.generic4.itda.controller;

import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;
import com.generic4.itda.dto.freelancer.FreelancerDashboardItem.DashboardProposalStatus;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.FreelancerDashboardService;
import com.generic4.itda.service.ResumeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/freelancers")
@RequiredArgsConstructor
public class FreelancerController {

    private final ResumeService resumeService;
    private final FreelancerDashboardService freelancerProjectService;

    @GetMapping("")
    public String index(@AuthenticationPrincipal ItDaPrincipal principal) {
        try {
            resumeService.findByEmail(principal.getEmail());
            return "redirect:/freelancers/dashboard";
        } catch (IllegalStateException ignored) {
            return "redirect:/resumes/new";
        }
    }

    @GetMapping("/dashboard")
    public String viewDashboard(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            Model model
    ) {
        populateModel(principal.getEmail(), status, q, model);
        return "freelancer/dashboard";
    }

    /** AJAX 탭 전환 — listSection fragment 만 반환 */
    @GetMapping("/dashboard/items")
    public String dashboardItems(
            @AuthenticationPrincipal ItDaPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            Model model
    ) {
        populateModel(principal.getEmail(), status, q, model);
        return "freelancer/dashboard :: #listSection";
    }

    // ─────────────────────────────────────────────────────────
    private void populateModel(String email, String status, String q, Model model) {
        List<FreelancerDashboardItem> allItems =
                freelancerProjectService.getDashboardItems(email);

        // 통계 카드는 전체 기준
        long totalCount      = allItems.size();
        long newCount        = allItems.stream().filter(i -> i.status() == DashboardProposalStatus.NEW).count();
        long inProgressCount = allItems.stream().filter(i -> i.status() == DashboardProposalStatus.IN_PROGRESS).count();
        long matchedCount    = allItems.stream().filter(i -> i.status() == DashboardProposalStatus.MATCHED).count();

        // 검색어 필터
        List<FreelancerDashboardItem> items = allItems;
        if (q != null && !q.isBlank()) {
            String kw = q.trim().toLowerCase();
            items = items.stream()
                    .filter(i -> i.proposalTitle().toLowerCase().contains(kw)
                              || i.companyName().toLowerCase().contains(kw))
                    .toList();
        }
        // 상태 필터
        if (status != null && !status.isBlank()) {
            try {
                DashboardProposalStatus fs = DashboardProposalStatus.valueOf(status);
                items = items.stream().filter(i -> i.status() == fs).toList();
            } catch (IllegalArgumentException ignored) { }
        }

        model.addAttribute("items",           items);
        model.addAttribute("totalCount",      totalCount);
        model.addAttribute("newCount",        newCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("matchedCount",    matchedCount);
        model.addAttribute("statusFilter",    status);
        model.addAttribute("query",           q);
    }
}
