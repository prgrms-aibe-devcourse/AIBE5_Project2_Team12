package com.generic4.itda.repository;

import com.generic4.itda.domain.recommendation.RecommendationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    void deleteAllByRecommendationRun_ProposalPosition_Proposal_Id(Long proposalId);
}
