package com.generic4.itda.repository;

import com.generic4.itda.domain.recommendation.RecommendationResult;
import java.util.List;
import java.util.Optional;
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

    @Query("""
            select rr
            from RecommendationResult rr
            join fetch rr.recommendationRun run
            join fetch run.proposalPosition pp
            join fetch pp.proposal p
            join fetch p.member proposalMember
            join fetch rr.resume resume
            join fetch resume.member resumeMember
            where rr.id = :resultId
            """)
    Optional<RecommendationResult> findDetailById(Long resultId);
}
