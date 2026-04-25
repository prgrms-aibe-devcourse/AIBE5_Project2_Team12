package com.generic4.itda.controller;

import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.dto.recommend.RecommendationRequestForm;
import com.generic4.itda.dto.recommend.RecommendationResultsViewModel;
import com.generic4.itda.dto.recommend.RecommendationResumeDetailViewModel;
import com.generic4.itda.dto.recommend.RecommendationRunHistoryViewModel;
import com.generic4.itda.dto.recommend.RecommendationRunStatusViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.service.recommend.RecommendationRunQueryService;
import com.generic4.itda.service.recommend.RecommendationRunService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
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

    private static String buildRunStatusEntryUrl(Long proposalId, Long runId, String backUrl) {
        if (StringUtils.hasText(backUrl)) {
            return buildRunStatusUrl(proposalId, runId, backUrl);
        }
        return buildRunStatusUrl(proposalId, runId, null, RUN_STATUS_FROM_PARAM);
    }

    @GetMapping("/proposals/{proposalId}/recommendations")
    public String entry(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal ItDaPrincipal principal
    ) {
        return "redirect:/proposals/" + proposalId + "?openRecommendModal=true";
    }

    @GetMapping("/proposal-positions/{proposalPositionId}/recommendations/runs")
    public String runHistory(
            @PathVariable Long proposalPositionId,
            @RequestParam Long proposalId,
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        String requestedBackUrl = normalizeLocalPath(backUrl);
        String resolvedBackUrl = resolveBackUrl(requestedBackUrl, "/proposals/" + proposalId);

        try {
            RecommendationRunHistoryViewModel view = recommendationRunQueryService
                    .getRecommendationRunHistory(proposalId, proposalPositionId, principal.getEmail());

            model.addAttribute("view", view);
            model.addAttribute("backUrl", resolvedBackUrl);
            model.addAttribute("currentUrl", buildRunHistoryUrl(proposalId, proposalPositionId, resolvedBackUrl));
            return "recommendation/runs";
        } catch (IllegalArgumentException e) {
            log.warn("추천 실행 이력 조회 실패. proposalId={}, proposalPositionId={}, email={}",
                    proposalId, proposalPositionId, principal.getEmail(), e);
            redirectAttributes.addFlashAttribute("errorMessage", toRunHistoryUserMessage(e));
            return redirectTo(requestedBackUrl, "/proposals/" + proposalId);
        }
    }

    /** 모달에서 fetch로 호출 — 성공: redirect URL 반환, 실패: 에러 메시지 반환 */
    @PostMapping(value = "/proposals/{proposalId}/recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> runAjax(
            @PathVariable Long proposalId,
            @Valid @ModelAttribute RecommendationRequestForm form,
            BindingResult bindingResult,
            @RequestParam(name = "backUrl", required = false) String backUrl,
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
                    "redirect", buildRunStatusEntryUrl(proposalId, runId, normalizeLocalPath(backUrl))
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
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        String requestedBackUrl = normalizeLocalPath(backUrl);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "포지션을 선택해주세요.");
            return redirectTo(requestedBackUrl, "/proposals/" + proposalId + "?openRecommendModal=true");
        }

        try {
            Long runId = recommendationRunService.createOrReuse(
                    proposalId,
                    form.proposalPositionId(),
                    principal.getEmail()
            );
            return "redirect:" + buildRunStatusEntryUrl(proposalId, runId, requestedBackUrl);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추천 실행 요청 실패. proposalId={}, proposalPositionId={}, email={}",
                    proposalId, form.proposalPositionId(), principal.getEmail(), e);

            redirectAttributes.addFlashAttribute("errorMessage", toUserMessage(e));
            return redirectTo(requestedBackUrl, "/proposals/" + proposalId + "?openRecommendModal=true");
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
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @AuthenticationPrincipal ItDaPrincipal principal
    ) {
        try {
            Long runId = recommendationRunService.createAdditional(
                    proposalId,
                    proposalPositionId,
                    principal.getEmail()
            );
            return ResponseEntity.ok(Map.of(
                    "redirect", buildRunStatusEntryUrl(proposalId, runId, normalizeLocalPath(backUrl))
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
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @AuthenticationPrincipal ItDaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        String requestedBackUrl = normalizeLocalPath(backUrl);
        try {
            Long runId = recommendationRunService.createAdditional(
                    proposalId,
                    proposalPositionId,
                    principal.getEmail()
            );
            return "redirect:" + buildRunStatusEntryUrl(proposalId, runId, requestedBackUrl);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추가 추천 요청 실패. proposalId={}, proposalPositionId={}, email={}",
                    proposalId, proposalPositionId, principal.getEmail(), e);

            redirectAttributes.addFlashAttribute("errorMessage", toUserMessage(e));
            return redirectTo(requestedBackUrl, "/proposals/" + proposalId + "?openRecommendModal=true");
        }
    }

    @GetMapping("/proposals/{proposalId}/runs/{runId}")
    public String runStatus(
            @PathVariable Long proposalId,
            @PathVariable Long runId,
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @RequestParam(name = "from", required = false) String from,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        String requestedBackUrl = normalizeLocalPath(backUrl);
        String resolvedBackUrl = resolveBackUrl(requestedBackUrl, "/proposals/" + proposalId);
        String normalizedFrom = (from != null && !from.isBlank()) ? from : null;

        try {
            RecommendationRunStatusViewModel view = recommendationRunQueryService
                    .getRecommendationRunStatus(proposalId, runId, principal.getEmail());

            if (view.status() == RecommendationRunStatus.COMPUTED && normalizedFrom == null) {
                return "redirect:" + resolveRunStatusNextActionUrl(view, resolvedBackUrl);
            }

            model.addAttribute("view", view);
            model.addAttribute("from", normalizedFrom);
            model.addAttribute("backUrl", resolvedBackUrl);
            model.addAttribute("refreshUrl", buildRunStatusUrl(proposalId, runId, requestedBackUrl, normalizedFrom));
            model.addAttribute("nextActionUrl", resolveRunStatusNextActionUrl(view, resolvedBackUrl));
            return "recommendation/status";
        } catch (IllegalArgumentException e) {
            log.warn("추천 실행 상태 조회 실패. proposalId={}, runId={}, email={}",
                    proposalId, runId, principal.getEmail(), e);

            model.addAttribute("title", "추천 실행 정보를 확인할 수 없습니다.");
            model.addAttribute("message", toRunStatusUserMessage(e));
            model.addAttribute("backUrl", resolvedBackUrl);

            return "recommendation/error";
        }
    }

    @GetMapping("/proposals/{proposalId}/recommendations/results")
    public String results(
            @PathVariable Long proposalId,
            @RequestParam Long runId,
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        String requestedBackUrl = normalizeLocalPath(backUrl);
        String resolvedBackUrl = resolveBackUrl(requestedBackUrl, "/proposals/" + proposalId);

        try {
            RecommendationResultsViewModel view = recommendationRunQueryService
                    .getRecommendationResults(proposalId, runId, principal.getEmail());

            model.addAttribute("view", view);
            model.addAttribute("backUrl", resolvedBackUrl);
            model.addAttribute("currentUrl", buildResultsUrl(proposalId, runId, resolvedBackUrl));
            return "recommendation/results";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추천 결과 조회 실패. proposalId={}, runId={}, email={}",
                    proposalId, runId, principal.getEmail(), e);

            model.addAttribute("title", "추천 결과를 확인할 수 없습니다.");
            model.addAttribute("message", toResultsUserMessage(e));
            model.addAttribute("backUrl", resolvedBackUrl);
            return "recommendation/error";
        }
    }

    @GetMapping("/proposals/{proposalId}/recommendations/results/{resultId}")
    public String candidateResume(
            @PathVariable Long proposalId,
            @PathVariable Long resultId,
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        String requestedBackUrl = normalizeLocalPath(backUrl);

        try {
            RecommendationResumeDetailViewModel view = recommendationRunQueryService
                    .getRecommendationCandidateResume(proposalId, resultId, principal.getEmail());

            model.addAttribute("view", view);
            model.addAttribute("backUrl", buildResultsUrl(proposalId, view.runId(), requestedBackUrl));
            return "recommendation/resume";
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("추천 후보 이력서 조회 실패. proposalId={}, resultId={}, email={}",
                    proposalId, resultId, principal.getEmail(), e);

            model.addAttribute("title", "추천 후보 이력서를 확인할 수 없습니다.");
            model.addAttribute("message", toResultsUserMessage(e));
            model.addAttribute("backUrl", resolveBackUrl(requestedBackUrl, "/proposals/" + proposalId + "?openRecommendModal=true"));
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

    private static String toRunHistoryUserMessage(IllegalArgumentException e) {
        return switch (e.getMessage()) {
            case "존재하지 않는 제안서입니다." -> "존재하지 않는 제안서입니다.";
            case "해당 제안서에 속한 모집 포지션이 아닙니다." -> "해당 제안서의 모집단위를 다시 확인해주세요.";
            case "접근 권한이 없습니다." -> "본인 제안서의 추천 실행 이력만 조회할 수 있습니다.";
            default -> "추천 실행 이력을 불러오는 중 문제가 발생했습니다.";
        };
    }

    private static String resolveRunStatusNextActionUrl(RecommendationRunStatusViewModel view, String backUrl) {
        return switch (view.status()) {
            case PENDING, RUNNING -> buildRunStatusUrl(view.proposalId(), view.runId(), backUrl);
            case COMPUTED -> buildResultsUrl(view.proposalId(), view.runId(), backUrl);
            case FAILED -> view.nextActionUrl();
        };
    }

    private static String buildRunHistoryUrl(Long proposalId, Long proposalPositionId, String backUrl) {
        String url = "/proposal-positions/%d/recommendations/runs?proposalId=%d"
                .formatted(proposalPositionId, proposalId);
        if (!StringUtils.hasText(backUrl)) {
            return url;
        }
        return url + "&backUrl=" + encodeParam(backUrl);
    }

    private static String buildRunStatusUrl(Long proposalId, Long runId, String backUrl) {
        return buildRunStatusUrl(proposalId, runId, backUrl, null);
    }

    private static String buildRunStatusUrl(Long proposalId, Long runId, String backUrl, String from) {
        StringBuilder url = new StringBuilder("/proposals/%d/runs/%d".formatted(proposalId, runId));
        boolean hasQuery = false;

        if (StringUtils.hasText(backUrl)) {
            url.append("?backUrl=").append(encodeParam(backUrl));
            hasQuery = true;
        }

        if (StringUtils.hasText(from)) {
            url.append(hasQuery ? "&" : "?")
                    .append("from=")
                    .append(encodeParam(from));
        }

        return url.toString();
    }

    private static String buildResultsUrl(Long proposalId, Long runId, String backUrl) {
        String url = "/proposals/%d/recommendations/results?runId=%d".formatted(proposalId, runId);
        if (!StringUtils.hasText(backUrl)) {
            return url;
        }
        return url + "&backUrl=" + encodeParam(backUrl);
    }

    private static String resolveBackUrl(String requestedBackUrl, String fallback) {
        return requestedBackUrl != null ? requestedBackUrl : fallback;
    }

    private static String normalizeLocalPath(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }

        String trimmed = candidate.trim();
        int duplicateValueSeparator = trimmed.indexOf(",/");
        if (duplicateValueSeparator > 0) {
            trimmed = trimmed.substring(0, duplicateValueSeparator);
        }

        if (trimmed.startsWith("/") && !trimmed.startsWith("//")) {
            return trimmed;
        }
        return null;
    }

    private static String redirectTo(String requestedBackUrl, String fallback) {
        return "redirect:" + resolveBackUrl(requestedBackUrl, fallback);
    }

    private static String encodeParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
