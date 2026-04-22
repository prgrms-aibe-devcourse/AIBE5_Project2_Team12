package com.generic4.itda.controller;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.MatchingQueryService;
import com.generic4.itda.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MatchingController {

    private final MatchingService matchingService;
    private final MatchingQueryService matchingQueryService;

    @GetMapping("/matchings/{matchingId}")
    public String detail(
            @PathVariable Long matchingId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            model.addAttribute("view", matchingQueryService.getDetail(matchingId, principal.getEmail()));
            return "matching/detail";
        } catch (IllegalArgumentException e) {
            log.warn("매칭 상세 조회 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 매칭입니다.");
            return "redirect:/";
        } catch (AccessDeniedException e) {
            log.warn("매칭 상세 접근 거부. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "해당 매칭 정보에 접근할 수 없습니다.");
            return "redirect:/";
        }
    }

    @PostMapping("/recommendation-results/{recommendationResultId}/matchings")
    public String request(
            @PathVariable Long recommendationResultId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @RequestParam(name = "errorRedirectTo", required = false) String errorRedirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.request(recommendationResultId, principal.getEmail());
            redirectAttributes.addFlashAttribute("noticeMessage", "매칭 요청을 보냈습니다.");
            String fallback = "/matchings/" + matching.getId();
            return redirectTo(redirectTo, fallback);
        } catch (IllegalArgumentException | AccessDeniedException | IllegalStateException e) {
            log.warn("매칭 요청 생성 실패. recommendationResultId={}, email={}",
                    recommendationResultId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toRequestUserMessage(e));
            return redirectTo(errorRedirectTo, "/client/dashboard");
        }
    }

    @PostMapping("/matchings/{matchingId}/accept")
    public String accept(
            @PathVariable Long matchingId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @RequestParam(name = "errorRedirectTo", required = false) String errorRedirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.accept(matchingId, principal.getEmail());
            redirectAttributes.addFlashAttribute("noticeMessage", "매칭 요청을 수락했습니다.");
            String fallback = "/matchings/" + matching.getId();
            return redirectTo(redirectTo, fallback);
        } catch (IllegalArgumentException | AccessDeniedException | IllegalStateException e) {
            log.warn("매칭 요청 수락 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toResponseUserMessage(e));
            return redirectTo(errorRedirectTo, "/freelancers/dashboard");
        }
    }

    @PostMapping("/matchings/{matchingId}/reject")
    public String reject(
            @PathVariable Long matchingId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @RequestParam(name = "errorRedirectTo", required = false) String errorRedirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.reject(matchingId, principal.getEmail());
            redirectAttributes.addFlashAttribute("noticeMessage", "매칭 요청을 거절했습니다.");
            String fallback = "/matchings/" + matching.getId();
            return redirectTo(redirectTo, fallback);
        } catch (IllegalArgumentException | AccessDeniedException | IllegalStateException e) {
            log.warn("매칭 요청 거절 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toResponseUserMessage(e));
            return redirectTo(errorRedirectTo, "/freelancers/dashboard");
        }
    }

    private static String toRequestUserMessage(RuntimeException e) {
        if (e instanceof IllegalArgumentException) {
            return "요청한 추천 결과를 찾을 수 없습니다.";
        }
        if (e instanceof AccessDeniedException) {
            return "본인 제안서에 대해서만 매칭 요청을 보낼 수 있습니다.";
        }
        if (e instanceof IllegalStateException) {
            return switch (e.getMessage()) {
                case "MATCHING 상태의 제안서에 대해서만 매칭 요청을 보낼 수 있습니다." ->
                        "추천이 진행 중인 제안서에서만 매칭 요청을 보낼 수 있습니다.";
                case "OPEN 상태의 모집 포지션에 대해서만 매칭 요청을 보낼 수 있습니다." ->
                        "모집 중인 포지션에서만 매칭 요청을 보낼 수 있습니다.";
                case "이미 요청했거나 진행 중인 매칭입니다." ->
                        "이미 요청했거나 진행 중인 후보입니다.";
                default -> "현재 상태에서는 매칭 요청을 보낼 수 없습니다.";
            };
        }
        return "매칭 요청 처리 중 문제가 발생했습니다.";
    }

    private static String toResponseUserMessage(RuntimeException e) {
        if (e instanceof IllegalArgumentException) {
            return "응답할 매칭 요청을 찾을 수 없습니다.";
        }
        if (e instanceof AccessDeniedException) {
            return "본인에게 온 매칭 요청에만 응답할 수 있습니다.";
        }
        if (e instanceof IllegalStateException) {
            return switch (e.getMessage()) {
                case "정원이 이미 찬 모집 포지션입니다." ->
                        "이미 정원이 마감된 포지션입니다.";
                case "종료된 모집 포지션에는 응답할 수 없습니다." ->
                        "모집이 종료된 포지션에는 응답할 수 없습니다.";
                case "제안 상태의 매칭만 수락할 수 있습니다.",
                        "제안 상태의 매칭만 거절할 수 있습니다." ->
                        "이미 처리된 매칭 요청입니다.";
                default -> "현재 상태에서는 매칭 요청에 응답할 수 없습니다.";
            };
        }
        return "매칭 요청 응답 처리 중 문제가 발생했습니다.";
    }

    private String redirectTo(String redirectTo, String fallback) {
        if (StringUtils.hasText(redirectTo) && redirectTo.startsWith("/") && !redirectTo.startsWith("//")) {
            return "redirect:" + redirectTo;
        }
        return "redirect:" + fallback;
    }
}
