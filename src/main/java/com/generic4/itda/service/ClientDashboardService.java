package com.generic4.itda.service;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.dto.client.ClientDashboardFilter;
import com.generic4.itda.dto.client.ClientDashboardProjectItem;
import com.generic4.itda.dto.client.ClientDashboardSummaryItem;
import com.generic4.itda.dto.client.ClientDashboardViewModel;
import com.generic4.itda.repository.ProposalRepository;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClientDashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final NumberFormat NUMBER_FORMATTER = NumberFormat.getNumberInstance(Locale.KOREA);

    private final ProposalRepository proposalRepository;

    public ClientDashboardViewModel getDashboard(String memberEmail, ClientDashboardFilter filter) {
        Assert.hasText(memberEmail, "회원 이메일은 필수값입니다.");

        ClientDashboardFilter selectedFilter = filter == null ? ClientDashboardFilter.ALL : filter;
        List<Proposal> proposals = loadProjects(memberEmail, selectedFilter);

        return new ClientDashboardViewModel(
                selectedFilter.getKey(),
                selectedFilter.getTitle(),
                buildSummaries(memberEmail, selectedFilter),
                proposals.stream()
                        .map(this::toProjectItem)
                        .toList()
        );
    }

    private List<Proposal> loadProjects(String memberEmail, ClientDashboardFilter filter) {
        if (filter.isAll()) {
            return proposalRepository.findAllWithPositionsByMemberEmail(memberEmail);
        }

        return proposalRepository.findAllWithPositionsByMemberEmailAndStatus(
                memberEmail,
                filter.toProposalStatus()
        );
    }

    private List<ClientDashboardSummaryItem> buildSummaries(String memberEmail, ClientDashboardFilter selectedFilter) {
        long totalCount = proposalRepository.countByMember_Email_Value(memberEmail);
        long waitingCount = proposalRepository.countByMember_Email_ValueAndStatus(memberEmail, ProposalStatus.WRITING);
        long matchingCount = proposalRepository.countByMember_Email_ValueAndStatus(memberEmail, ProposalStatus.MATCHING);
        long completeCount = proposalRepository.countByMember_Email_ValueAndStatus(memberEmail, ProposalStatus.COMPLETE);

        return List.of(
                toSummaryItem(ClientDashboardFilter.ALL, totalCount, selectedFilter),
                toSummaryItem(ClientDashboardFilter.WAITING, waitingCount, selectedFilter),
                toSummaryItem(ClientDashboardFilter.MATCHING, matchingCount, selectedFilter),
                toSummaryItem(ClientDashboardFilter.COMPLETE, completeCount, selectedFilter)
        );
    }

    private ClientDashboardSummaryItem toSummaryItem(
            ClientDashboardFilter filter,
            long count,
            ClientDashboardFilter selectedFilter
    ) {
        return new ClientDashboardSummaryItem(
                filter.getKey(),
                filter.getTitle(),
                count,
                filter.getDescription(),
                filter == selectedFilter
        );
    }

    private ClientDashboardProjectItem toProjectItem(Proposal proposal) {
        return new ClientDashboardProjectItem(
                proposal.getId(),
                proposal.getTitle(),
                proposal.getStatus().name(),
                proposal.getStatus().getDescription(),
                proposal.getPositions().size(),
                formatBudget(proposal.getTotalBudgetMin(), proposal.getTotalBudgetMax()),
                proposal.getModifiedAt().format(DATE_FORMATTER),
                buildMatchingOverview(proposal.getStatus())
        );
    }

    private String formatBudget(Long min, Long max) {
        if (min == null && max == null) {
            return "예산 미정";
        }
        if (min != null && max != null && min.equals(max)) {
            return NUMBER_FORMATTER.format(min) + "원";
        }
        if (min != null && max != null) {
            return NUMBER_FORMATTER.format(min) + " ~ " + NUMBER_FORMATTER.format(max) + "원";
        }
        if (min != null) {
            return NUMBER_FORMATTER.format(min) + "원 이상";
        }
        return NUMBER_FORMATTER.format(max) + "원 이하";
    }

    private String buildMatchingOverview(ProposalStatus status) {
        return switch (status) {
            case WRITING -> "매칭 시작 전";
            case MATCHING -> "추천/매칭 진행 중";
            case COMPLETE -> "프로젝트 종료";
        };
    }
}
