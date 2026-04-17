package com.generic4.itda.repository;

import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecommendationRunRepository extends JpaRepository<RecommendationRun, Long> {

    Optional<RecommendationRun> findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
            Long proposalPositionId,
            String requestFingerprint,
            RecommendationAlgorithm algorithm
    );

    @Query("""
            select rr
            from RecommendationRun rr
            join fetch rr.proposalPosition pp
            join fetch pp.proposal p
            join fetch p.member m
            where rr.id = :runId
            """)
    Optional<RecommendationRun> findDetailById(Long runId);
}
