package com.generic4.itda.service;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingCancellationReason;
import com.generic4.itda.domain.matching.constant.MatchingParticipantRole;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
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
    private static final EnumSet<MatchingStatus> OCCUPIED_MATCHING_STATUSES = EnumSet.of(
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

    public Matching accept(Long matchingId, String freelancerEmail) {
        Matching matching = matchingRepository.findDetailById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 요청을 찾을 수 없습니다. id=" + matchingId));

        validateFreelancerOwnership(matching, freelancerEmail);
        validatePositionRespondable(matching.getProposalPosition());
        validateCapacityAvailable(matching.getProposalPosition());

        matching.accept();
        updatePositionStatusAfterAccept(matching.getProposalPosition());
        return matching;
    }

    public Matching reject(Long matchingId, String freelancerEmail) {
        Matching matching = matchingRepository.findDetailById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 요청을 찾을 수 없습니다. id=" + matchingId));

        validateFreelancerOwnership(matching, freelancerEmail);
        matching.reject();
        return matching;
    }

    public Matching acceptContractStart(Long matchingId, String email) {
        Matching matching = getMatchingDetail(matchingId);
        MatchingParticipantRole participantRole = resolveParticipantRole(matching, email);

        matching.acceptContractStart(participantRole);
        return matching;
    }

    public Matching requestCancellation(
            Long matchingId,
            String email,
            MatchingCancellationReason reason,
            String reasonDetail
    ) {
        Matching matching = getMatchingDetail(matchingId);
        MatchingParticipantRole participantRole = resolveParticipantRole(matching, email);

        matching.requestCancellation(participantRole, reason, reasonDetail);
        return matching;
    }

    public Matching withdrawCancellation(Long matchingId, String email) {
        Matching matching = getMatchingDetail(matchingId);
        MatchingParticipantRole participantRole = resolveParticipantRole(matching, email);

        matching.withdrawCancellation(participantRole);
        return matching;
    }

    public Matching confirmCancellation(Long matchingId, String email) {
        Matching matching = getMatchingDetail(matchingId);
        MatchingParticipantRole participantRole = resolveParticipantRole(matching, email);

        matching.confirmCancellation(participantRole);
        recalculatePositionStatus(matching.getProposalPosition());
        return matching;
    }

    public Matching submitReview(Long matchingId, String email, String review) {
        Matching matching = getMatchingDetail(matchingId);
        MatchingParticipantRole participantRole = resolveParticipantRole(matching, email);

        matching.submitReview(participantRole, review);
        return matching;
    }

    public Matching confirmCompletion(Long matchingId, String email) {
        Matching matching = getMatchingDetail(matchingId);
        MatchingParticipantRole participantRole = resolveParticipantRole(matching, email);
        MatchingStatus beforeStatus = matching.getStatus();

        matching.confirmCompletion(participantRole);
        if (beforeStatus != matching.getStatus()) {
            recalculatePositionStatus(matching.getProposalPosition());
        }
        return matching;
    }

    public int cancelOverdueAcceptedCancellationRequests(LocalDateTime now) {
        List<Matching> matchings = matchingRepository.findAcceptedCancellationRequestsDue(now);
        int canceledCount = 0;
        for (Matching matching : matchings) {
            if (matching.cancelAutomaticallyIfOverdue(now)) {
                recalculatePositionStatus(matching.getProposalPosition());
                canceledCount++;
            }
        }
        return canceledCount;
    }

    private Matching getMatchingDetail(Long matchingId) {
        return matchingRepository.findDetailById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 요청을 찾을 수 없습니다. id=" + matchingId));
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

    private void validateFreelancerOwnership(Matching matching, String freelancerEmail) {
        if (!matching.getFreelancerMember().getEmail().getValue().equals(freelancerEmail)) {
            throw new AccessDeniedException("본인에게 온 매칭 요청에만 응답할 수 있습니다.");
        }
    }

    private MatchingParticipantRole resolveParticipantRole(Matching matching, String email) {
        if (matching.getClientMember().getEmail().getValue().equals(email)) {
            return MatchingParticipantRole.CLIENT;
        }
        if (matching.getFreelancerMember().getEmail().getValue().equals(email)) {
            return MatchingParticipantRole.FREELANCER;
        }
        throw new AccessDeniedException("해당 매칭의 당사자만 처리할 수 있습니다.");
    }

    private void validatePositionRespondable(ProposalPosition proposalPosition) {
        if (proposalPosition.getStatus() == ProposalPositionStatus.CLOSED) {
            throw new IllegalStateException("종료된 모집 포지션에는 응답할 수 없습니다.");
        }
        if (proposalPosition.getStatus() == ProposalPositionStatus.FULL) {
            throw new IllegalStateException("정원이 이미 찬 모집 포지션입니다.");
        }
    }

    private void validateCapacityAvailable(ProposalPosition proposalPosition) {
        Long headCount = proposalPosition.getHeadCount();
        if (headCount == null) {
            return;
        }

        long occupiedCount = matchingRepository.countByProposalPosition_IdAndStatusIn(
                proposalPosition.getId(),
                OCCUPIED_MATCHING_STATUSES
        );

        if (occupiedCount >= headCount) {
            throw new IllegalStateException("정원이 이미 찬 모집 포지션입니다.");
        }
    }

    private void updatePositionStatusAfterAccept(ProposalPosition proposalPosition) {
        Long headCount = proposalPosition.getHeadCount();
        if (headCount == null) {
            return;
        }

        long occupiedCount = matchingRepository.countByProposalPosition_IdAndStatusIn(
                proposalPosition.getId(),
                OCCUPIED_MATCHING_STATUSES
        );

        if (occupiedCount >= headCount) {
            proposalPosition.changeStatus(ProposalPositionStatus.FULL);
        }
    }

    private void recalculatePositionStatus(ProposalPosition proposalPosition) {
        if (proposalPosition.getStatus() == ProposalPositionStatus.CLOSED) {
            return;
        }

        Long headCount = proposalPosition.getHeadCount();
        if (headCount == null) {
            return;
        }

        long occupiedCount = matchingRepository.countByProposalPosition_IdAndStatusIn(
                proposalPosition.getId(),
                OCCUPIED_MATCHING_STATUSES
        );

        proposalPosition.changeStatus(occupiedCount >= headCount
                ? ProposalPositionStatus.FULL
                : ProposalPositionStatus.OPEN);
    }
}
