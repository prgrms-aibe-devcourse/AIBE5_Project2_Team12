package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.LlmStatus;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.dto.recommend.RecommendationCandidateItem;
import com.generic4.itda.dto.recommend.RecommendationResultsViewModel;
import com.generic4.itda.dto.recommend.RecommendationRunStatusViewModel;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationRunQueryService {

    private final RecommendationRunRepository recommendationRunRepository;
    private final RecommendationResultRepository recommendationResultRepository;

    public RecommendationRunStatusViewModel getRecommendationRunStatus(Long proposalId, Long runId, String email) {
        RecommendationRun run = recommendationRunRepository.findDetailById(runId)
                .orElseThrow(() -> new IllegalArgumentException("추천 실행 정보를 찾을 수 없습니다."));

        ProposalPosition proposalPosition = run.getProposalPosition();
        Proposal proposal = proposalPosition.getProposal();

        validateProposalMatches(proposal, proposalId);
        validateOwnership(proposal, email);

        return toViewModel(proposal, run);
    }

    public RecommendationResultsViewModel getRecommendationResults(Long proposalId, Long runId, String email) {
        RecommendationRun run = recommendationRunRepository.findDetailById(runId)
                .orElseThrow(() -> new IllegalArgumentException("추천 실행 정보를 찾을 수 없습니다."));

        ProposalPosition proposalPosition = run.getProposalPosition();
        Proposal proposal = proposalPosition.getProposal();

        validateProposalMatches(proposal, proposalId);
        validateOwnership(proposal, email);

        if (!run.isComputed()) {
            throw new IllegalStateException("추천 결과가 아직 준비되지 않았습니다.");
        }

        List<RecommendationResult> results = recommendationResultRepository.findByRunIdWithResume(runId);

        List<RecommendationCandidateItem> candidates = results.stream()
                .map(this::toCandidateItem)
                .toList();

        return new RecommendationResultsViewModel(
                proposal.getId(),
                run.getId(),
                proposal.getTitle(),
                resolvePositionTitle(proposalPosition),
                run.getTopK(),
                run.getCandidateCount(),
                candidates
        );
    }

    private RecommendationCandidateItem toCandidateItem(RecommendationResult result) {
        Resume resume = result.getResume();
        Member member = resume.getMember();
        ReasonFacts facts = result.getReasonFacts();

        String rawName = member.getNickname() != null && !member.getNickname().isBlank()
                ? member.getNickname()
                : member.getName();
        String displayName = maskName(rawName);

        String introduction = resume.getIntroduction() != null ? resume.getIntroduction() : "";

        int careerYears = facts != null && facts.careerYears() != null
                ? facts.careerYears()
                : (resume.getCareerYears() != null ? (int) resume.getCareerYears() : 0);

        String workTypeLabel = resume.getPreferredWorkType() != null
                ? resume.getPreferredWorkType().getDescription()
                : "미정";

        int finalPct = result.getFinalScore() != null
                ? result.getFinalScore().multiply(BigDecimal.valueOf(100)).intValue()
                : 0;
        int embedPct = result.getEmbeddingScore() != null
                ? result.getEmbeddingScore().multiply(BigDecimal.valueOf(100)).intValue()
                : 0;

        List<String> matchedSkills = facts != null && facts.matchedSkills() != null
                ? facts.matchedSkills() : List.of();
        List<String> highlights = facts != null && facts.highlights() != null
                ? facts.highlights() : List.of();

        return new RecommendationCandidateItem(
                result.getId(),
                result.getRank(),
                displayName,  // masked
                introduction,
                careerYears,
                workTypeLabel,
                finalPct,
                embedPct,
                matchedSkills,
                highlights,
                result.getLlmReason(),
                result.getLlmStatus().getDescription(),
                result.getLlmStatus() == LlmStatus.READY
        );
    }

    private static String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    private static String resolvePositionTitle(ProposalPosition position) {
        if (position.getTitle() != null && !position.getTitle().isBlank()) {
            return position.getTitle();
        }
        return position.getPosition().getName();
    }

    private void validateProposalMatches(Proposal proposal, Long proposalId) {
        if (!proposal.getId().equals(proposalId)) {
            throw new IllegalArgumentException("잘못된 추천 실행 접근입니다.");
        }
    }

    private void validateOwnership(Proposal proposal, String email) {
        if (!proposal.getMember().getEmail().getValue().equals(email)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
    }

    private RecommendationRunStatusViewModel toViewModel(
            Proposal proposal,
            RecommendationRun run
    ) {
        return switch (run.getStatus()) {
            case PENDING -> new RecommendationRunStatusViewModel(
                    proposal.getId(),
                    run.getId(),
                    proposal.getTitle(),
                    run.getStatus(),
                    "추천 요청이 접수되었습니다.",
                    "선택한 포지션의 추천 결과를 준비하고 있습니다.",
                    "새로고침",
                    "/proposals/%d/runs/%d".formatted(proposal.getId(), run.getId()),
                    true
            );
            case RUNNING -> new RecommendationRunStatusViewModel(
                    proposal.getId(),
                    run.getId(),
                    proposal.getTitle(),
                    run.getStatus(),
                    "추천을 계산하고 있습니다.",
                    "잠시 후 추천 결과를 확인할 수 있습니다.",
                    "새로고침",
                    "/proposals/%d/runs/%d".formatted(proposal.getId(), run.getId()),
                    true
            );
            case COMPUTED -> new RecommendationRunStatusViewModel(
                    proposal.getId(),
                    run.getId(),
                    proposal.getTitle(),
                    run.getStatus(),
                    "추천 결과가 준비되었습니다.",
                    "추천 결과 목록으로 이동할 수 있습니다.",
                    "결과 보기",
                    "/proposals/%d/recommendations/results?runId=%d".formatted(proposal.getId(), run.getId()),
                    false
            );
            case FAILED -> new RecommendationRunStatusViewModel(
                    proposal.getId(),
                    run.getId(),
                    proposal.getTitle(),
                    run.getStatus(),
                    "추천 생성에 실패했습니다.",
                    "잠시 후 다시 시도해 주세요.",
                    "추천 다시 실행",
                    "/proposals/%d/recommendations".formatted(proposal.getId()),
                    false
            );
        };
    }
}
