package com.generic4.itda.controller;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.dto.matching.ClientMatchingListItemViewModel;
import com.generic4.itda.dto.matching.ClientMatchingListViewModel;
import com.generic4.itda.dto.matching.ClientMatchingListViewModel.PositionFilterItem;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.service.ProposalService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/proposals/{proposalId}/matchings")
@RequiredArgsConstructor
public class MatchingEntryController {

    private final ProposalService proposalService;
    private final MatchingRepository matchingRepository;

    /**
     * 단일 매칭 목록 페이지 — 포지션/상태 필터를 쿼리 파라미터로 받아 초기 렌더링
     */
    @GetMapping
    public String list(
            @PathVariable Long proposalId,
            @RequestParam(name = "positionId", required = false) Long positionId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Proposal proposal = proposalService.findOwnedProposal(proposalId, principal.getEmail());
            List<Matching> matchings = matchingRepository
                    .findWithPositionAndFreelancerByProposalIdAndClientEmail(proposalId, principal.getEmail());

            MatchingStatus filterStatus = parseStatus(status).orElse(null);
            String resolvedBackUrl = resolveBackUrl(proposalId, backUrl);

            List<ClientMatchingListItemViewModel> items = applyFiltersAndSort(
                    matchings, positionId, filterStatus);

            model.addAttribute("view", new ClientMatchingListViewModel(
                    proposal.getId(),
                    proposal.getTitle(),
                    buildPositionFilters(proposal, matchings),
                    positionId,
                    filterStatus != null ? filterStatus.name() : null,
                    items
            ));
            model.addAttribute("items", items);
            model.addAttribute("backUrl", resolvedBackUrl);
            model.addAttribute("detailBackUrl",
                    buildListUrl(proposalId, positionId, filterStatus, resolvedBackUrl));
            return "matching/list";
        } catch (ProposalNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 제안서입니다.");
            return "redirect:/client/dashboard";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인 제안서만 조회할 수 있습니다.");
            return "redirect:/client/dashboard";
        }
    }

    /**
     * AJAX 필터 요청 — 포지션/상태 필터 변경 시 목록 영역만 교체
     */
    @GetMapping("/items")
    public String listItems(
            @PathVariable Long proposalId,
            @RequestParam(name = "positionId", required = false) Long positionId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "backUrl", required = false) String backUrl,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model
    ) {
        proposalService.findOwnedProposal(proposalId, principal.getEmail());

        List<Matching> matchings = matchingRepository
                .findWithPositionAndFreelancerByProposalIdAndClientEmail(proposalId, principal.getEmail());

        MatchingStatus filterStatus = parseStatus(status).orElse(null);

        model.addAttribute("items", applyFiltersAndSort(matchings, positionId, filterStatus));
        model.addAttribute("detailBackUrl",
                buildListUrl(proposalId, positionId, filterStatus, resolveBackUrl(proposalId, backUrl)));
        return "matching/fragments :: itemList";
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────

    private List<ClientMatchingListItemViewModel> applyFiltersAndSort(
            List<Matching> matchings,
            Long positionId,
            MatchingStatus filterStatus
    ) {
        return matchings.stream()
                .filter(m -> positionId == null || m.getProposalPosition().getId().equals(positionId))
                .filter(m -> filterStatus == null || m.getStatus() == filterStatus)
                .sorted(matchingSortComparator())
                .map(this::toListItem)
                .toList();
    }

    private List<PositionFilterItem> buildPositionFilters(Proposal proposal, List<Matching> allMatchings) {
        Map<Long, Long> countByPosition = allMatchings.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getProposalPosition().getId(),
                        Collectors.counting()
                ));

        return proposal.getPositions().stream()
                .sorted(Comparator.comparing(ProposalPosition::getId))
                .map(position -> new PositionFilterItem(
                        position.getId(),
                        resolvePositionTitle(position),
                        countByPosition.getOrDefault(position.getId(), 0L)
                ))
                .toList();
    }

    private ClientMatchingListItemViewModel toListItem(Matching matching) {
        String freelancerName = StringUtils.hasText(matching.getFreelancerMember().getNickname())
                ? matching.getFreelancerMember().getNickname().trim()
                : matching.getFreelancerMember().getName();

        return new ClientMatchingListItemViewModel(
                matching.getId(),
                matching.getProposalPosition().getId(),
                resolvePositionTitle(matching.getProposalPosition()),
                freelancerName,
                matching.getStatus(),
                matching.getStatus().getDescription(),
                resolveRequestedAt(matching)
        );
    }

    private static LocalDateTime resolveRequestedAt(Matching matching) {
        return matching.getRequestedAt() != null ? matching.getRequestedAt() : matching.getCreatedAt();
    }

    private static String resolvePositionTitle(ProposalPosition position) {
        if (StringUtils.hasText(position.getTitle())) {
            return position.getTitle().trim();
        }
        return position.getPosition().getName();
    }

    private static Optional<MatchingStatus> parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return Optional.empty();
        }
        try {
            return Optional.of(MatchingStatus.valueOf(status.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String resolveBackUrl(Long proposalId, String backUrl) {
        if (StringUtils.hasText(backUrl)) {
            String trimmed = backUrl.trim();
            if (trimmed.startsWith("/") && !trimmed.startsWith("//")) {
                return trimmed;
            }
        }
        return "/proposals/" + proposalId;
    }

    private static String buildListUrl(
            Long proposalId,
            Long positionId,
            MatchingStatus filterStatus,
            String backUrl
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/proposals/{proposalId}/matchings");
        if (positionId != null) {
            builder.queryParam("positionId", positionId);
        }
        if (filterStatus != null) {
            builder.queryParam("status", filterStatus.name());
        }
        if (StringUtils.hasText(backUrl)) {
            builder.queryParam("backUrl", backUrl);
        }
        return builder.buildAndExpand(proposalId).toUriString();
    }

    private static Comparator<Matching> matchingSortComparator() {
        Map<MatchingStatus, Integer> weight = Map.of(
                MatchingStatus.PROPOSED, 0,
                MatchingStatus.IN_PROGRESS, 1,
                MatchingStatus.ACCEPTED, 2,
                MatchingStatus.REJECTED, 3,
                MatchingStatus.COMPLETED, 4,
                MatchingStatus.CANCELED, 5
        );
        return Comparator
                .comparing((Matching m) -> weight.getOrDefault(m.getStatus(), 99))
                .thenComparing(Matching::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Matching::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
