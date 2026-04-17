package com.generic4.itda.repository;

import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
            select rr.id
            from RecommendationRun rr
            where rr.status = com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus.PENDING
            order by rr.id asc
            """)
    List<Long> findPendingRunIds(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RecommendationRun rr
            set rr.status = com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus.RUNNING,
                rr.errorMessage = null
            where rr.id = :runId
            and rr.status = com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus.PENDING
            """)
    int claimAsRunning(Long runId);
}
