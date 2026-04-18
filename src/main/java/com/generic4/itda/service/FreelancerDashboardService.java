package com.generic4.itda.service;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;
import com.generic4.itda.dto.freelancer.FreelancerDashboardItem.DashboardProposalStatus;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.generic4.itda.domain.proposal.QProposalPosition.proposalPosition;
import static com.generic4.itda.domain.recommendation.QRecommendationResult.recommendationResult;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FreelancerDashboardService {

    private final MatchingRepository matchingRepository;

    @Transactional(readOnly = true)
    public List<FreelancerDashboardItem> getDashboardItems(String email, String status, String q) {
        return matchingRepository.getDashboardItems(email, status, q);
    }
}
