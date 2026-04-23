package com.generic4.itda.repository;

import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.service.recommend.CandidatePoolRow;
import java.util.List;

public interface ResumeQueryRepository {

    List<CandidatePoolRow> findCandidatePool(
            ProposalPosition proposalPosition,
            List<Long> requiredSkillIds,
            int candidatePoolSize
    );

    List<Long> findRecommendableResumeIds(
            ProposalPosition proposalPosition,
            int candidatePoolSize
    );

    List<CandidatePoolRow> findCandidatePool(
            ProposalPosition proposalPosition,
            List<Long> requiredSkillIds,
            List<Long> excludedResumeIds,
            int candidatePoolSize
    );

    List<Long> findRecommendableResumeIds(
            ProposalPosition proposalPosition,
            List<Long> excludedResumeIds,
            int candidatePoolSize
    );
}
