package com.generic4.itda.controller;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/recommendation-results/{recommendationResultId}/matchings")
    public String request(
            @PathVariable Long recommendationResultId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.request(recommendationResultId, principal.getEmail());
            redirectAttributes.addFlashAttribute("noticeMessage", "매칭 요청을 보냈습니다.");
            String fallback = "/proposals/" + matching.getProposalPosition().getProposal().getId();
            return redirectTo(redirectTo, fallback);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "요청한 추천 결과를 찾을 수 없습니다.");
            return redirectTo(redirectTo, "/client/dashboard");
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return redirectTo(redirectTo, "/client/dashboard");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return redirectTo(redirectTo, "/client/dashboard");
        }
    }

    @PostMapping("/matchings/{matchingId}/accept")
    public String accept(
            @PathVariable Long matchingId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.accept(matchingId, principal.getEmail());
            redirectAttributes.addFlashAttribute("noticeMessage", "매칭 요청을 수락했습니다.");
            String fallback = "/proposals/" + matching.getProposalPosition().getProposal().getId();
            return redirectTo(redirectTo, fallback);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "응답할 매칭 요청을 찾을 수 없습니다.");
            return redirectTo(redirectTo, "/freelancers/dashboard");
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return redirectTo(redirectTo, "/freelancers/dashboard");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return redirectTo(redirectTo, "/freelancers/dashboard");
        }
    }

    @PostMapping("/matchings/{matchingId}/reject")
    public String reject(
            @PathVariable Long matchingId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.reject(matchingId, principal.getEmail());
            redirectAttributes.addFlashAttribute("noticeMessage", "매칭 요청을 거절했습니다.");
            String fallback = "/proposals/" + matching.getProposalPosition().getProposal().getId();
            return redirectTo(redirectTo, fallback);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "응답할 매칭 요청을 찾을 수 없습니다.");
            return redirectTo(redirectTo, "/freelancers/dashboard");
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return redirectTo(redirectTo, "/freelancers/dashboard");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return redirectTo(redirectTo, "/freelancers/dashboard");
        }
    }

    private String redirectTo(String redirectTo, String fallback) {
        if (StringUtils.hasText(redirectTo) && redirectTo.startsWith("/") && !redirectTo.startsWith("//")) {
            return "redirect:" + redirectTo;
        }
        return "redirect:" + fallback;
    }
}
