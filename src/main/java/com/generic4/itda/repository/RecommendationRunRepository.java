package com.generic4.itda.repository;

import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRunRepository extends JpaRepository<RecommendationRun, Long> {

    Optional<RecommendationRun> findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
            Long proposalPositionId,
            String requestFingerprint,
            RecommendationAlgorithm algorithm
    );
}
