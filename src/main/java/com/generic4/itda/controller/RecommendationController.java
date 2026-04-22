package com.generic4.itda.controller;

import com.generic4.itda.dto.recommend.RecommendationEntryViewModel;
import com.generic4.itda.dto.recommend.RecommendationRequestForm;
import com.generic4.itda.dto.recommend.RecommendationResultsViewModel;
import com.generic4.itda.dto.recommend.RecommendationRunStatusViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.recommend.RecommendationEntryService;
import com.generic4.itda.service.recommend.RecommendationRunQueryService;
import com.generic4.itda.service.recommend.RecommendationRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationEntryService recommendationEntryService;
    private final RecommendationRunService recommendationRunService;
    private final RecommendationRunQueryService recommendationRunQueryService;

    @GetMapping("/proposals/{proposalId}/recommendations")
    public String entry(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        RecommendationEntryViewModel entry = recommendationEntryService.getEntry(proposalId, principal.getEmail());

        model.addAttribute("entry", entry);
        model.addAttribute("requestForm", new RecommendationRequestForm(entry.selectedProposalPositionId()));

        return "recommendation/entry";
    }

    @PostMapping("/proposals/{proposalId}/recommendations")
    public String run(
            @PathVariable Long proposalId,
            @Valid @ModelAttribute("requestForm") RecommendationRequestForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            RecommendationEntryViewModel entry = recommendationEntryService.getEntry(proposalId, principal.getEmail());
            model.addAttribute("entry", entry);
            return "recommendation/entry";
        }

        try {
            Long runId = recommendationRunService.createOrReuse(
                    proposalId,
                    form.proposalPositionId(),
                    principal.getEmail()
            );

            redirectAttributes.addAttribute("runId", runId);
            redirectAttributes.addAttribute("proposalId", proposalId);
            return "redirect:/proposals/{proposalId}/runs/{runId}";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추천 실행 요청 실패. proposalId={}, proposalPositionId={}, email={}",
                    proposalId, form.proposalPositionId(), principal.getEmail(), e);

            redirectAttributes.addFlashAttribute("errorMessage", toUserMessage(e));
            return "redirect:/proposals/{proposalId}/recommendations";
        }
    }

    @GetMapping("/proposals/{proposalId}/runs/{runId}")
    public String runStatus(
            @PathVariable Long proposalId,
            @PathVariable Long runId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        try {
            RecommendationRunStatusViewModel view = recommendationRunQueryService
                    .getRecommendationRunStatus(proposalId, runId, principal.getEmail());

            model.addAttribute("view", view);
            return "recommendation/status";
        } catch (IllegalArgumentException e) {
            log.warn("추천 실행 상태 조회 실패. proposalId={}, runId={}, email={}",
                    proposalId, runId, principal.getEmail(), e);

            model.addAttribute("title", "추천 실행 정보를 확인할 수 없습니다.");
            model.addAttribute("message", toRunStatusUserMessage(e));
            model.addAttribute("backUrl", "/proposals/" + proposalId + "/recommendations");

            return "recommendation/error";
        }
    }

    @GetMapping("/proposals/{proposalId}/recommendations/results")
    public String results(
            @PathVariable Long proposalId,
            @RequestParam Long runId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        try {
            RecommendationResultsViewModel view = recommendationRunQueryService
                    .getRecommendationResults(proposalId, runId, principal.getEmail());

            model.addAttribute("view", view);
            return "recommendation/results";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추천 결과 조회 실패. proposalId={}, runId={}, email={}",
                    proposalId, runId, principal.getEmail(), e);

            model.addAttribute("title", "추천 결과를 확인할 수 없습니다.");
            model.addAttribute("message", toResultsUserMessage(e));
            model.addAttribute("backUrl", "/proposals/" + proposalId + "/runs/" + runId);
            return "recommendation/error";
        }
    }

    private static String toUserMessage(RuntimeException e) {
        if (e instanceof IllegalArgumentException) {
            return "추천 요청을 처리할 수 없습니다. 선택한 포지션을 다시 확인해주세요.";
        }
        if (e instanceof IllegalStateException) {
            return "현재 상태에는 추천을 실행할 수 없습니다.";
        }
        return "추천 요청 처리 중 문제가 발생했습니다. 다시 시도해주세요.";
    }

    private static String toResultsUserMessage(RuntimeException e) {
        if (e instanceof IllegalStateException) {
            return "추천이 아직 완료되지 않았습니다. 잠시 후 다시 시도해주세요.";
        }
        return "추천 결과를 불러오는 중 문제가 발생했습니다.";
    }

    private static String toRunStatusUserMessage(IllegalArgumentException e) {
        return switch (e.getMessage()) {
            case "추천 실행 정보를 찾을 수 없습니다." -> "존재하지 않거나 만료된 추천 실행입니다.";
            case "잘못된 추천 실행 접근입니다." -> "요청한 추천 실행 정보가 올바르지 않습니다.";
            case "접근 권한이 없습니다." -> "해당 추천 실행 정보에 접근할 수 없습니다.";
            default -> "추천 실행 정보를 불러오는 중 문제가 발생했습니다.";
        };
    }
}
