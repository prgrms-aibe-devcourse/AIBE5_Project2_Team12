package com.generic4.itda.controller;

import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.dto.recommend.RecommendationRequestForm;
import com.generic4.itda.dto.recommend.RecommendationResultsViewModel;
import com.generic4.itda.dto.recommend.RecommendationResumeDetailViewModel;
import com.generic4.itda.dto.recommend.RecommendationRunStatusViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.recommend.RecommendationRunQueryService;
import com.generic4.itda.service.recommend.RecommendationRunService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private static final String RUN_STATUS_FROM_PARAM = "recommendation";

    private final RecommendationRunService recommendationRunService;
    private final RecommendationRunQueryService recommendationRunQueryService;

    private static String buildRunStatusRedirectUrl(Long proposalId, Long runId) {
        return "/proposals/" + proposalId + "/runs/" + runId + "?from=" + RUN_STATUS_FROM_PARAM;
    }

    @GetMapping("/proposals/{proposalId}/recommendations")
    public String entry(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal
    ) {
        return "redirect:/proposals/" + proposalId + "?openRecommendModal=true";
    }

    /** 모달에서 fetch로 호출 — 성공: redirect URL 반환, 실패: 에러 메시지 반환 */
    @PostMapping(value = "/proposals/{proposalId}/recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> runAjax(
            @PathVariable Long proposalId,
            @Valid @ModelAttribute RecommendationRequestForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal ItDaPrincipal principal
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "포지션을 선택해주세요."));
        }

        try {
            Long runId = recommendationRunService.createOrReuse(
                    proposalId,
                    form.proposalPositionId(),
                    principal.getEmail()
            );
            return ResponseEntity.ok(Map.of(
                    "redirect", buildRunStatusRedirectUrl(proposalId, runId)
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추천 실행 요청 실패(AJAX). proposalId={}, proposalPositionId={}, email={}",
                    proposalId, form.proposalPositionId(), principal.getEmail(), e);
            return ResponseEntity.badRequest().body(Map.of("error", toUserMessage(e)));
        }
    }

    @PostMapping("/proposals/{proposalId}/recommendations")
    public String run(
            @PathVariable Long proposalId,
            @Valid @ModelAttribute("requestForm") RecommendationRequestForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "포지션을 선택해주세요.");
            return "redirect:/proposals/" + proposalId + "?openRecommendModal=true";
        }

        try {
            Long runId = recommendationRunService.createOrReuse(
                    proposalId,
                    form.proposalPositionId(),
                    principal.getEmail()
            );

            redirectAttributes.addAttribute("runId", runId);
            redirectAttributes.addAttribute("proposalId", proposalId);
            redirectAttributes.addAttribute("from", RUN_STATUS_FROM_PARAM);
            return "redirect:/proposals/{proposalId}/runs/{runId}";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추천 실행 요청 실패. proposalId={}, proposalPositionId={}, email={}",
                    proposalId, form.proposalPositionId(), principal.getEmail(), e);

            redirectAttributes.addFlashAttribute("errorMessage", toUserMessage(e));
            return "redirect:/proposals/" + proposalId + "?openRecommendModal=true";
        }
    }

    /** 모달에서 fetch로 호출 — 성공: redirect URL 반환, 실패: 에러 메시지 반환 */
    @PostMapping(
            value = "/proposal-positions/{proposalPositionId}/recommendations/more",
            produces = MediaType.APPLICATION_JSON_VALUE,
            headers = "Accept=application/json"
    )
    @ResponseBody
    public ResponseEntity<Map<String, String>> moreAjax(
            @PathVariable Long proposalPositionId,
            @RequestParam Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal
    ) {
        try {
            Long runId = recommendationRunService.createAdditional(
                    proposalId,
                    proposalPositionId,
                    principal.getEmail()
            );
            return ResponseEntity.ok(Map.of(
                    "redirect", buildRunStatusRedirectUrl(proposalId, runId)
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추가 추천 요청 실패(AJAX). proposalId={}, proposalPositionId={}, email={}",
                    proposalId, proposalPositionId, principal.getEmail(), e);
            return ResponseEntity.badRequest().body(Map.of("error", toUserMessage(e)));
        }
    }

    @PostMapping("/proposal-positions/{proposalPositionId}/recommendations/more")
    public String more(
            @PathVariable Long proposalPositionId,
            @RequestParam Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Long runId = recommendationRunService.createAdditional(
                    proposalId,
                    proposalPositionId,
                    principal.getEmail()
            );

            redirectAttributes.addAttribute("runId", runId);
            redirectAttributes.addAttribute("proposalId", proposalId);
            redirectAttributes.addAttribute("from", RUN_STATUS_FROM_PARAM);
            return "redirect:/proposals/{proposalId}/runs/{runId}";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추가 추천 요청 실패. proposalId={}, proposalPositionId={}, email={}",
                    proposalId, proposalPositionId, principal.getEmail(), e);

            redirectAttributes.addFlashAttribute("errorMessage", toUserMessage(e));
            return "redirect:/proposals/" + proposalId + "?openRecommendModal=true";
        }
    }

    @GetMapping("/proposals/{proposalId}/runs/{runId}")
    public String runStatus(
            @PathVariable Long proposalId,
            @PathVariable Long runId,
            @RequestParam(name = "from", required = false) String from,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        String normalizedFrom = (from != null && !from.isBlank()) ? from : null;

        try {
            RecommendationRunStatusViewModel view = recommendationRunQueryService
                    .getRecommendationRunStatus(proposalId, runId, principal.getEmail());

            if (view.status() == RecommendationRunStatus.COMPUTED && normalizedFrom == null) {
                return "redirect:" + view.nextActionUrl();
            }

            model.addAttribute("view", view);
            model.addAttribute("from", normalizedFrom);
            return "recommendation/status";
        } catch (IllegalArgumentException e) {
            log.warn("추천 실행 상태 조회 실패. proposalId={}, runId={}, email={}",
                    proposalId, runId, principal.getEmail(), e);

            model.addAttribute("title", "추천 실행 정보를 확인할 수 없습니다.");
            model.addAttribute("message", toRunStatusUserMessage(e));
            model.addAttribute("backUrl", "/proposals/" + proposalId + "?openRecommendModal=true");

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

    @GetMapping("/proposals/{proposalId}/recommendations/results/{resultId}")
    public String candidateResume(
            @PathVariable Long proposalId,
            @PathVariable Long resultId,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        try {
            RecommendationResumeDetailViewModel view = recommendationRunQueryService
                    .getRecommendationCandidateResume(proposalId, resultId, principal.getEmail());

            model.addAttribute("view", view);
            return "recommendation/resume";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추천 후보 이력서 조회 실패. proposalId={}, resultId={}, email={}",
                    proposalId, resultId, principal.getEmail(), e);

            model.addAttribute("title", "추천 후보 이력서를 확인할 수 없습니다.");
            model.addAttribute("message", toResultsUserMessage(e));
            model.addAttribute("backUrl", "/proposals/" + proposalId + "?openRecommendModal=true");
            return "recommendation/error";
        }
    }

    private static String toUserMessage(RuntimeException e) {
        if (e instanceof IllegalArgumentException) {
            return "추천 요청을 처리할 수 없습니다. 선택한 포지션을 다시 확인해주세요.";
        }
        if (e instanceof IllegalStateException) {
            return "모집 중 상태의 포지션만 추천을 실행할 수 있습니다.";
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
