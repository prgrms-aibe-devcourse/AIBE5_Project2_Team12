package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.dto.recommend.RecommendationEntryPositionItem;
import com.generic4.itda.dto.recommend.RecommendationEntrySkillItem;
import com.generic4.itda.dto.recommend.RecommendationEntryViewModel;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ProposalRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationEntryService {

    private final ProposalRepository proposalRepository;
    private final MemberRepository memberRepository;

    public RecommendationEntryViewModel getEntry(Long proposalId, String email) {
        Proposal proposal = loadProposalDetail(proposalId);

        Member member = Optional.ofNullable(memberRepository.findByEmail_Value(email))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        validateOwnership(proposal, member);

        boolean runnable = proposal.getStatus() == ProposalStatus.MATCHING;

        List<RecommendationEntryPositionItem> positions = proposal.getPositions().stream()
                .sorted(Comparator.comparing(ProposalPosition::getId))
                .map(this::toPositionItem)
                .toList();

        Long selectedProposalPositionId = positions.isEmpty() ? null : positions.get(0).proposalPositionId();

        return new RecommendationEntryViewModel(
                proposal.getId(),
                proposal.getTitle(),
                proposal.getStatus().getDescription(),
                runnable,
                buildHelperMessage(runnable),
                selectedProposalPositionId,
                positions
        );
    }

    private Proposal loadProposalDetail(Long proposalId) {
        Proposal proposal = proposalRepository.findWithPositionsById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 제안서입니다."));
        proposalRepository.findPositionsWithSkillsByProposalId(proposalId);
        return proposal;
    }

    private static void validateOwnership(Proposal proposal, Member member) {
        // member는 id 기반 equals & hashcode가 아님
        if (!proposal.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인 제안서만 조회할 수 있습니다.");
        }
    }

    private RecommendationEntryPositionItem toPositionItem(ProposalPosition position) {
        return new RecommendationEntryPositionItem(
                position.getId(),
                resolvePositionTitle(position),
                position.getPosition().getName(),
                position.getHeadCount(),
                formatBudget(position.getUnitBudgetMin(), position.getUnitBudgetMax()),
                position.getExpectedPeriod(),
                position.getSkills().stream()
                        .map(skill -> new RecommendationEntrySkillItem(
                                skill.getSkill().getName(),
                                skill.getImportance().getDescription()
                        )).toList(),
                position.getStatus().name(),
                position.getStatus().getDescription()
        );
    }

    private String resolvePositionTitle(ProposalPosition position) {
        if (position.getTitle() != null && !position.getTitle().isBlank()) {
            return position.getTitle();
        }
        return position.getPosition().getName();
    }

    private String formatBudget(Long min, Long max) {
        if (min == null && max == null) {
            return "예산 미정";
        }
        if (min != null && max != null) {
            return String.format("%,d ~ %,d", min, max);
        }
        if (min != null) {
            return String.format("%,d 이상", min);
        }
        return String.format("%,d 이하", max);
    }

    private String buildHelperMessage(boolean runnable) {
        return runnable
                ? "선택한 포지션 기준으로 추천을 시작할 수 있습니다."
                : "제안서가 MATCHING 상태일 때만 추천을 실행할 수 있습니다.";
    }
}
