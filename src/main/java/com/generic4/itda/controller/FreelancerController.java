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
    private final FreelancerDashboardService freelancerDashboardService;

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
        // 1. 통계용 데이터 (필터링 전 전체 리스트)
        List<FreelancerDashboardItem> allItems = freelancerDashboardService.getDashboardItems(email, null, null);

        // 2. 화면 목록용 데이터 (검색어와 상태가 반영된 리스트 - DB에서 처리)
        List<FreelancerDashboardItem> filteredItems = freelancerDashboardService.getDashboardItems(email, status, q);

        // 통계 계산은 전체 데이터 기준
        model.addAttribute("totalCount",      allItems.size());
        model.addAttribute("newCount",        allItems.stream().filter(i -> i.getStatus() == DashboardProposalStatus.NEW).count());
        model.addAttribute("inProgressCount", allItems.stream().filter(i -> i.getStatus() == DashboardProposalStatus.IN_PROGRESS).count());
        model.addAttribute("matchedCount",    allItems.stream().filter(i -> i.getStatus() == DashboardProposalStatus.COMPLETED).count());

        // 화면 출력은 필터링된 데이터
        model.addAttribute("items",           filteredItems);
        model.addAttribute("statusFilter",    status);
        model.addAttribute("query",           q);
    }
}
