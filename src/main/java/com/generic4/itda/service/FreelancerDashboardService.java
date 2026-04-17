package com.generic4.itda.service;

import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;
import com.generic4.itda.dto.freelancer.FreelancerDashboardItem.DashboardProposalStatus;
import com.generic4.itda.repository.RecommendationResultRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FreelancerDashboardService {

    private final RecommendationResultRepository resultRepository;

    public List<FreelancerDashboardItem> getDashboardItems(String email) {
        return resultRepository.findAllByResumeOwnerEmail(email)
                .stream()
                .map(this::toItem)
                .toList();
    }

    private FreelancerDashboardItem toItem(RecommendationResult result) {
        var position = result.getRecommendationRun().getProposalPosition();
        var proposal = position.getProposal();

        List<String> skillNames = position.getSkills().stream()
                .map(pps -> pps.getSkill().getName())
                .toList();

        return new FreelancerDashboardItem(
                proposal.getId(),
                proposal.getTitle(),
                proposal.getMember().getName(),
                position.getPosition().getName(),
                resolveStatus(proposal.getStatus()),
                proposal.getTotalBudgetMin(),
                proposal.getTotalBudgetMax(),
                result.getCreatedAt().toLocalDate(),
                skillNames
        );
    }

    private DashboardProposalStatus resolveStatus(ProposalStatus s) {
        return switch (s) {
            case MATCHING -> DashboardProposalStatus.IN_PROGRESS;
            case COMPLETE -> DashboardProposalStatus.MATCHED;
            default       -> DashboardProposalStatus.NEW;
        };
    }
}
