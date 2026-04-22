package com.generic4.itda.repository;

import com.generic4.itda.domain.recommendation.RecommendationResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    void deleteAllByRecommendationRun_ProposalPosition_Proposal_Id(Long proposalId);

    @Query("SELECT r FROM RecommendationResult r " +
            "JOIN FETCH r.resume res " +
            "JOIN FETCH res.member " +
            "WHERE r.recommendationRun.id = :runId " +
            "ORDER BY r.rank ASC")
    List<RecommendationResult> findByRunIdWithResume(@Param("runId") Long runId);
}
