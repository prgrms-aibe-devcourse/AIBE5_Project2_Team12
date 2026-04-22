package com.generic4.itda.service;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MatchingService {

    private static final EnumSet<MatchingStatus> ACTIVE_MATCHING_STATUSES = EnumSet.of(
            MatchingStatus.PROPOSED,
            MatchingStatus.ACCEPTED,
            MatchingStatus.IN_PROGRESS
    );

    private final RecommendationResultRepository recommendationResultRepository;
    private final MatchingRepository matchingRepository;

    public Matching request(Long recommendationResultId, String clientEmail) {
        RecommendationResult recommendationResult = recommendationResultRepository.findDetailById(recommendationResultId)
                .orElseThrow(() -> new IllegalArgumentException("추천 결과를 찾을 수 없습니다. id=" + recommendationResultId));

        ProposalPosition proposalPosition = recommendationResult.getRecommendationRun().getProposalPosition();
        Proposal proposal = proposalPosition.getProposal();
        Resume resume = recommendationResult.getResume();

        validateOwnership(proposal, clientEmail);
        validateProposalStatus(proposal);
        validatePositionStatus(proposalPosition);
        validateNoActiveMatching(proposalPosition, resume);

        Matching matching = Matching.create(
                resume,
                proposalPosition,
                proposal.getMember(),
                resume.getMember()
        );
        return matchingRepository.save(matching);
    }

    private void validateOwnership(Proposal proposal, String clientEmail) {
        if (!proposal.getMember().getEmail().getValue().equals(clientEmail)) {
            throw new AccessDeniedException("본인 제안서에 대해서만 매칭 요청을 보낼 수 있습니다.");
        }
    }

    private void validateProposalStatus(Proposal proposal) {
        if (proposal.getStatus() != ProposalStatus.MATCHING) {
            throw new IllegalStateException("MATCHING 상태의 제안서에 대해서만 매칭 요청을 보낼 수 있습니다.");
        }
    }

    private void validatePositionStatus(ProposalPosition proposalPosition) {
        if (proposalPosition.getStatus() != ProposalPositionStatus.OPEN) {
            throw new IllegalStateException("OPEN 상태의 모집 포지션에 대해서만 매칭 요청을 보낼 수 있습니다.");
        }
    }

    private void validateNoActiveMatching(ProposalPosition proposalPosition, Resume resume) {
        boolean hasActiveMatching = matchingRepository.existsByProposalPosition_IdAndResume_IdAndStatusIn(
                proposalPosition.getId(),
                resume.getId(),
                ACTIVE_MATCHING_STATUSES
        );

        if (hasActiveMatching) {
            throw new IllegalStateException("이미 요청했거나 진행 중인 매칭입니다.");
        }
    }
}
