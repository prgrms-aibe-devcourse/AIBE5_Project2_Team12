package com.generic4.itda.controller;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.dto.matching.ClientMatchingFreelancerItemViewModel;
import com.generic4.itda.dto.matching.ClientMatchingFreelancerSelectionViewModel;
import com.generic4.itda.dto.matching.ClientMatchingPositionItemViewModel;
import com.generic4.itda.dto.matching.ClientMatchingPositionSelectionViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.service.ProposalService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/proposals/{proposalId}/matchings")
@RequiredArgsConstructor
public class MatchingEntryController {

    private static final EnumSet<MatchingStatus> ACTIVE_STATUSES = EnumSet.of(
            MatchingStatus.PROPOSED,
            MatchingStatus.ACCEPTED,
            MatchingStatus.IN_PROGRESS
    );

    private final ProposalService proposalService;
    private final MatchingRepository matchingRepository;

    @GetMapping
    public String selectPosition(
            @PathVariable Long proposalId,
            @RequestParam(name = "openOnly", defaultValue = "false") boolean openOnly,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Proposal proposal = proposalService.findOwnedProposal(proposalId, principal.getEmail());

            List<Matching> matchings = matchingRepository
                    .findByProposalPosition_Proposal_IdAndClientMember_Email_Value(proposalId, principal.getEmail());

            Map<Long, List<Matching>> byPositionId = matchings.stream()
                    .collect(Collectors.groupingBy(matching -> matching.getProposalPosition().getId()));

            List<ClientMatchingPositionItemViewModel> positions = proposal.getPositions().stream()
                    .filter(position -> !openOnly || position.getStatus() == ProposalPositionStatus.OPEN)
                    .sorted(positionSortComparator())
                    .map(position -> toPositionItem(position, byPositionId.getOrDefault(position.getId(), List.of())))
                    .toList();

            model.addAttribute("view", new ClientMatchingPositionSelectionViewModel(
                    proposal.getId(),
                    proposal.getTitle(),
                    openOnly,
                    positions
            ));
            return "matching/positions";
        } catch (ProposalNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 제안서입니다.");
            return "redirect:/client/dashboard";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인 제안서만 조회할 수 있습니다.");
            return "redirect:/client/dashboard";
        }
    }

    @GetMapping("/positions/{proposalPositionId}")
    public String selectFreelancer(
            @PathVariable Long proposalId,
            @PathVariable Long proposalPositionId,
            @RequestParam(name = "status", required = false) String status,
            @AuthenticationPrincipal ItDaPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Proposal proposal = proposalService.findOwnedProposal(proposalId, principal.getEmail());

            ProposalPosition proposalPosition = findPosition(proposal, proposalPositionId)
                    .orElseThrow(() -> new IllegalArgumentException("잘못된 포지션입니다."));

            List<Matching> matchings = matchingRepository
                    .findWithFreelancerMemberByProposalPositionIdAndClientEmail(proposalPositionId, principal.getEmail());

            MatchingStatus filterStatus = parseStatus(status).orElse(null);

            List<ClientMatchingFreelancerItemViewModel> items = matchings.stream()
                    .filter(matching -> filterStatus == null || matching.getStatus() == filterStatus)
                    .sorted(matchingSortComparator())
                    .map(this::toFreelancerItem)
                    .toList();

            model.addAttribute("view", new ClientMatchingFreelancerSelectionViewModel(
                    proposal.getId(),
                    proposal.getTitle(),
                    proposalPosition.getId(),
                    resolvePositionTitle(proposalPosition),
                    toPositionStatusLabel(proposalPosition.getStatus()),
                    filterStatus != null ? filterStatus.name() : "",
                    availableFilterStatuses(),
                    items
            ));
            return "matching/freelancers";
        } catch (ProposalNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 제안서입니다.");
            return "redirect:/client/dashboard";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인 제안서만 조회할 수 있습니다.");
            return "redirect:/client/dashboard";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/proposals/" + proposalId + "/matchings";
        }
    }

    private static List<MatchingStatus> availableFilterStatuses() {
        return List.of(
                MatchingStatus.PROPOSED,
                MatchingStatus.ACCEPTED,
                MatchingStatus.IN_PROGRESS,
                MatchingStatus.COMPLETED
        );
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

    private static Optional<ProposalPosition> findPosition(Proposal proposal, Long proposalPositionId) {
        if (proposal.getPositions() == null || proposal.getPositions().isEmpty()) {
            return Optional.empty();
        }
        return proposal.getPositions().stream()
                .filter(position -> position.getId().equals(proposalPositionId))
                .findFirst();
    }

    private static ClientMatchingPositionItemViewModel toPositionItem(ProposalPosition position, Collection<Matching> matchings) {
        long requestedCount = matchings.size();
        long activeCount = matchings.stream()
                .map(Matching::getStatus)
                .filter(ACTIVE_STATUSES::contains)
                .count();

        return new ClientMatchingPositionItemViewModel(
                position.getId(),
                resolvePositionTitle(position),
                position.getStatus(),
                toPositionStatusLabel(position.getStatus()),
                requestedCount,
                activeCount
        );
    }

    private ClientMatchingFreelancerItemViewModel toFreelancerItem(Matching matching) {
        String freelancerName = matching.getFreelancerMember().getNickname() != null
                && !matching.getFreelancerMember().getNickname().isBlank()
                ? matching.getFreelancerMember().getNickname().trim()
                : matching.getFreelancerMember().getName();

        return new ClientMatchingFreelancerItemViewModel(
                matching.getId(),
                freelancerName,
                matching.getStatus(),
                matching.getStatus().getDescription(),
                resolveRequestedAt(matching)
        );
    }

    private static LocalDateTime resolveRequestedAt(Matching matching) {
        if (matching.getRequestedAt() != null) {
            return matching.getRequestedAt();
        }
        return matching.getCreatedAt();
    }

    private static Comparator<ProposalPosition> positionSortComparator() {
        Map<ProposalPositionStatus, Integer> weight = Map.of(
                ProposalPositionStatus.OPEN, 0,
                ProposalPositionStatus.FULL, 1,
                ProposalPositionStatus.CLOSED, 2
        );
        return Comparator
                .comparing((ProposalPosition position) -> weight.getOrDefault(position.getStatus(), 99))
                .thenComparing(ProposalPosition::getId);
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
                .comparing((Matching matching) -> weight.getOrDefault(matching.getStatus(), 99))
                .thenComparing(Matching::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Matching::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static String resolvePositionTitle(ProposalPosition position) {
        if (StringUtils.hasText(position.getTitle())) {
            return position.getTitle().trim();
        }
        return position.getPosition().getName();
    }

    private static String toPositionStatusLabel(ProposalPositionStatus status) {
        if (status == null) {
            return "미정";
        }
        return switch (status) {
            case OPEN -> "모집 중";
            case FULL -> "모집 완료";
            case CLOSED -> "모집 종료";
        };
    }
}
