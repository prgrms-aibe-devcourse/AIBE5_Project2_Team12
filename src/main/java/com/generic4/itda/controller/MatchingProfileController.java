package com.generic4.itda.controller;

import com.generic4.itda.dto.profile.ProfileShellViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.MatchingProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MatchingProfileController {

    private final MatchingProfileQueryService matchingProfileQueryService;

    @GetMapping("/matchings/{matchingId}/counterpart-profile")
    public String counterpartProfile(
            @PathVariable Long matchingId,
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ProfileShellViewModel view = matchingProfileQueryService
                    .getCounterpartProfile(matchingId, principal.getEmail());

            model.addAttribute("view", view.withBackUrl(resolveBackUrl(backUrl, view.backUrl())));

            return "profile/shell";
        } catch (IllegalArgumentException e) {
            log.warn("상대 프로필 조회 실패. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 매칭입니다.");
            return redirectTo(backUrl, "/matchings/" + matchingId);
        } catch (AccessDeniedException e) {
            log.warn("상대 프로필 접근 거부. matchingId={}, email={}",
                    matchingId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "해당 매칭 정보에 접근할 수 없습니다.");
            return redirectTo(backUrl, "/matchings/" + matchingId);
        }
    }

    private String redirectTo(String backUrl, String fallback) {
        return "redirect:" + resolveBackUrl(backUrl, fallback);
    }

    private String resolveBackUrl(String backUrl, String fallback) {
        if (StringUtils.hasText(backUrl) && backUrl.startsWith("/") && !backUrl.startsWith("//")) {
            return backUrl;
        }
        return fallback;
    }
}
