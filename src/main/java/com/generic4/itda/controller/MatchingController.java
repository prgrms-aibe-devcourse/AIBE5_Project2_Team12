package com.generic4.itda.controller;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.dto.matching.MatchingCancellationRequestForm;
import com.generic4.itda.dto.matching.MatchingReviewRequestForm;
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
import org.springframework.web.bind.annotation.ModelAttribute;
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

    @PostMapping("/matchings/{matchingId}/contract-start/accept")
    public String acceptContractStart(
            @PathVariable Long matchingId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @RequestParam(name = "errorRedirectTo", required = false) String errorRedirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.acceptContractStart(matchingId, principal.getEmail());
            redirectAttributes.addFlashAttribute(
                    "noticeMessage",
                    matching.getStatus() == MatchingStatus.IN_PROGRESS
                            ? "양측 계약 시작 확인이 완료되어 프로젝트가 진행 중으로 전환되었습니다."
                            : "계약 시작 확인을 저장했습니다."
            );
            return redirectTo(redirectTo, "/matchings/" + matching.getId());
        } catch (IllegalArgumentException | AccessDeniedException | IllegalStateException e) {
            log.warn("계약 시작 확인 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toLifecycleUserMessage(e));
            return redirectTo(errorRedirectTo, "/matchings/" + matchingId);
        }
    }

    @PostMapping("/matchings/{matchingId}/cancellations")
    public String requestCancellation(
            @PathVariable Long matchingId,
            @ModelAttribute MatchingCancellationRequestForm form,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @RequestParam(name = "errorRedirectTo", required = false) String errorRedirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.requestCancellation(
                    matchingId,
                    principal.getEmail(),
                    form.reason(),
                    form.reasonDetail()
            );
            redirectAttributes.addFlashAttribute("noticeMessage", "취소 요청을 보냈습니다.");
            return redirectTo(redirectTo, "/matchings/" + matching.getId());
        } catch (IllegalArgumentException | AccessDeniedException | IllegalStateException e) {
            log.warn("매칭 취소 요청 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toLifecycleUserMessage(e));
            return redirectTo(errorRedirectTo, "/matchings/" + matchingId);
        }
    }

    @PostMapping("/matchings/{matchingId}/cancellations/withdraw")
    public String withdrawCancellation(
            @PathVariable Long matchingId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @RequestParam(name = "errorRedirectTo", required = false) String errorRedirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.withdrawCancellation(matchingId, principal.getEmail());
            redirectAttributes.addFlashAttribute("noticeMessage", "취소 요청을 철회했습니다.");
            return redirectTo(redirectTo, "/matchings/" + matching.getId());
        } catch (IllegalArgumentException | AccessDeniedException | IllegalStateException e) {
            log.warn("매칭 취소 요청 철회 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toLifecycleUserMessage(e));
            return redirectTo(errorRedirectTo, "/matchings/" + matchingId);
        }
    }

    @PostMapping("/matchings/{matchingId}/cancellations/confirm")
    public String confirmCancellation(
            @PathVariable Long matchingId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @RequestParam(name = "errorRedirectTo", required = false) String errorRedirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.confirmCancellation(matchingId, principal.getEmail());
            redirectAttributes.addFlashAttribute("noticeMessage", "취소 요청을 확인하여 매칭이 취소되었습니다.");
            return redirectTo(redirectTo, "/matchings/" + matching.getId());
        } catch (IllegalArgumentException | AccessDeniedException | IllegalStateException e) {
            log.warn("매칭 취소 확인 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toLifecycleUserMessage(e));
            return redirectTo(errorRedirectTo, "/matchings/" + matchingId);
        }
    }

    @PostMapping("/matchings/{matchingId}/reviews")
    public String submitReview(
            @PathVariable Long matchingId,
            @ModelAttribute MatchingReviewRequestForm form,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @RequestParam(name = "errorRedirectTo", required = false) String errorRedirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.submitReview(matchingId, principal.getEmail(), form.review());
            redirectAttributes.addFlashAttribute("noticeMessage", "후기를 저장했습니다.");
            return redirectTo(redirectTo, "/matchings/" + matching.getId());
        } catch (IllegalArgumentException | AccessDeniedException | IllegalStateException e) {
            log.warn("매칭 후기 저장 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toLifecycleUserMessage(e));
            return redirectTo(errorRedirectTo, "/matchings/" + matchingId);
        }
    }

    @PostMapping("/matchings/{matchingId}/completion/confirm")
    public String confirmCompletion(
            @PathVariable Long matchingId,
            @RequestParam(name = "redirectTo", required = false) String redirectTo,
            @RequestParam(name = "errorRedirectTo", required = false) String errorRedirectTo,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Matching matching = matchingService.confirmCompletion(matchingId, principal.getEmail());
            redirectAttributes.addFlashAttribute(
                    "noticeMessage",
                    matching.getStatus() == MatchingStatus.COMPLETED
                            ? "양측 완료 확인이 끝나 프로젝트가 완료되었습니다."
                            : "프로젝트 완료 확인을 저장했습니다."
            );
            return redirectTo(redirectTo, "/matchings/" + matching.getId());
        } catch (IllegalArgumentException | AccessDeniedException | IllegalStateException e) {
            log.warn("프로젝트 완료 확인 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toLifecycleUserMessage(e));
            return redirectTo(errorRedirectTo, "/matchings/" + matchingId);
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

    private static String toLifecycleUserMessage(RuntimeException e) {
        if (e instanceof IllegalArgumentException) {
            return switch (e.getMessage()) {
                case "취소 사유는 필수입니다." ->
                        "취소 사유를 선택해주세요.";
                case "현재 취소 상황에 맞지 않는 사유입니다." ->
                        "현재 상태에 맞는 취소 사유를 선택해주세요.";
                case "기타 취소 사유를 입력해주세요." ->
                        "기타 사유를 선택한 경우 상세 사유를 입력해주세요.";
                case "후기 내용은 필수입니다." ->
                        "후기 내용을 입력해주세요.";
                default -> "요청한 매칭 정보를 찾을 수 없습니다.";
            };
        }
        if (e instanceof AccessDeniedException) {
            return "해당 매칭의 당사자만 처리할 수 있습니다.";
        }
        if (e instanceof IllegalStateException) {
            return switch (e.getMessage()) {
                case "수락된 매칭만 계약 시작을 수락할 수 있습니다." ->
                        "매칭 수락 이후에만 계약 시작을 확인할 수 있습니다.";
                case "취소 요청 중인 매칭은 계약 시작을 수락할 수 없습니다." ->
                        "취소 요청이 진행 중인 매칭은 계약 시작을 확인할 수 없습니다.";
                case "수락 또는 진행 중인 매칭만 취소를 요청할 수 있습니다." ->
                        "수락 또는 진행 중인 매칭만 취소 요청을 보낼 수 있습니다.";
                case "이미 취소 요청이 진행 중입니다." ->
                        "이미 진행 중인 취소 요청이 있습니다.";
                case "진행 중인 취소 요청이 없습니다." ->
                        "진행 중인 취소 요청이 없습니다.";
                case "취소 요청자만 취소 요청을 철회할 수 있습니다." ->
                        "취소 요청자만 요청을 철회할 수 있습니다.";
                case "취소 요청자는 취소 확인을 할 수 없습니다." ->
                        "취소 요청을 받은 상대방만 취소를 확인할 수 있습니다.";
                case "진행 중인 매칭에서만 후기를 작성할 수 있습니다." ->
                        "프로젝트 진행 중 상태에서만 후기를 작성할 수 있습니다.";
                case "진행 중인 매칭만 완료 확인을 할 수 있습니다." ->
                        "프로젝트 진행 중 상태에서만 완료를 확인할 수 있습니다.";
                case "후기 작성 후 프로젝트 완료를 확인할 수 있습니다." ->
                        "후기 작성 후 프로젝트 완료를 확인할 수 있습니다.";
                default -> "현재 상태에서는 요청을 처리할 수 없습니다.";
            };
        }
        return "매칭 상태 변경 중 문제가 발생했습니다.";
    }

    private String redirectTo(String redirectTo, String fallback) {
        if (StringUtils.hasText(redirectTo) && redirectTo.startsWith("/") && !redirectTo.startsWith("//")) {
            return "redirect:" + redirectTo;
        }
        return "redirect:" + fallback;
    }
}
