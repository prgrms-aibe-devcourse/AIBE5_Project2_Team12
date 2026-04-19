package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RecommendationRunService {

    private static final RecommendationAlgorithm DEFAULT_ALGORITHM = RecommendationAlgorithm.HEURISTIC_V1;
    private static final int DEFAULT_TOP_K = 3;

    private final ProposalRepository proposalRepository;
    private final MemberRepository memberRepository;
    private final RecommendationRunRepository recommendationRunRepository;
    private final RecommendationFingerprintGenerator fingerprintGenerator;

    public Long createOrReuse(Long proposalId, Long proposalPositionId, String email) {
        Proposal proposal = loadProposalDetail(proposalId);

        Member member = Optional.ofNullable(memberRepository.findByEmail_Value(email))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        validateOwnership(proposal, member);
        validateProposalStatus(proposal);

        ProposalPosition proposalPosition = proposal.getPositions().stream()
                .filter(position -> position.getId().equals(proposalPositionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 제안서에 속한 모집 포지션이 아닙니다."));

        validatePositionStatus(proposalPosition);

        String fingerprint = fingerprintGenerator.generate(
                proposalPosition,
                DEFAULT_ALGORITHM,
                DEFAULT_TOP_K
        );

        return recommendationRunRepository
                .findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                        proposalPositionId,
                        fingerprint,
                        DEFAULT_ALGORITHM
                )
                .map(RecommendationRun::getId)
                .orElseGet(() -> createNewRun(proposalPosition, fingerprint).getId());
    }

    private RecommendationRun createNewRun(ProposalPosition proposalPosition, String fingerprint) {
        RecommendationRun recommendationRun = RecommendationRun.create(
                proposalPosition,
                fingerprint,
                DEFAULT_ALGORITHM,
                DEFAULT_TOP_K
        );
        return recommendationRunRepository.save(recommendationRun);
    }

    private Proposal loadProposalDetail(Long proposalId) {
        Proposal proposal = proposalRepository.findWithPositionsById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 제안서입니다."));

        // Proposal -> positions, position -> skills 를 한 번에 fetch join 하면
        // 컬렉션 중복 조회로 row 폭증과 순서 불안정 문제가 생길 수 있어
        // skills 는 별도 조회로 영속성 컨텍스트에 적재한다.
        proposalRepository.findPositionsWithSkillsByProposalId(proposalId);
        return proposal;
    }

    private void validateOwnership(Proposal proposal, Member member) {
        // member는 id 기반 equals & hashcode가 아님
        if (!proposal.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인 제안서만 조회할 수 있습니다.");
        }
    }

    private void validateProposalStatus(Proposal proposal) {
        if (proposal.getStatus() != ProposalStatus.MATCHING) {
            throw new IllegalStateException("MATCHING 상태의 제안서만 추천을 실행할 수 있습니다.");
        }
    }

    private void validatePositionStatus(ProposalPosition proposalPosition) {
        if (proposalPosition.getStatus() != ProposalPositionStatus.OPEN) {
            throw new IllegalStateException("OPEN 상태의 모집 포지션만 추천을 실행할 수 있습니다.");
        }
    }
}
