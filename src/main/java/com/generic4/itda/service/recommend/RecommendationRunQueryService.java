package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.LlmStatus;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeSkill;
import com.generic4.itda.dto.recommend.RecommendationCandidateItem;
import com.generic4.itda.dto.recommend.RecommendationResumeCareerItem;
import com.generic4.itda.dto.recommend.RecommendationResumeDetailViewModel;
import com.generic4.itda.dto.recommend.RecommendationResumeSkillItem;
import com.generic4.itda.dto.recommend.RecommendationResultsViewModel;
import com.generic4.itda.dto.recommend.RecommendationRunStatusViewModel;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationRunQueryService {

    private final RecommendationRunRepository recommendationRunRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final MatchingRepository matchingRepository;

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

        // 후보별 매칭 상태를 한 번의 쿼리로 배치 조회 (N+1 방지)
        List<Long> resumeIds = results.stream()
                .map(r -> r.getResume().getId())
                .toList();
        Map<Long, MatchingStatus> matchingStatusMap = loadMatchingStatusMap(
                proposalPosition.getId(), resumeIds);

        List<RecommendationCandidateItem> candidates = results.stream()
                .map(r -> toCandidateItem(r, matchingStatusMap.get(r.getResume().getId())))
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

    public RecommendationResumeDetailViewModel getRecommendationCandidateResume(Long proposalId, Long resultId, String email) {
        RecommendationResult result = recommendationResultRepository.findDetailById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("추천 결과를 찾을 수 없습니다."));

        RecommendationRun run = result.getRecommendationRun();
        ProposalPosition proposalPosition = run.getProposalPosition();
        Proposal proposal = proposalPosition.getProposal();

        validateProposalMatches(proposal, proposalId);
        validateOwnership(proposal, email);

        if (!run.isComputed()) {
            throw new IllegalStateException("추천 결과가 아직 준비되지 않았습니다.");
        }

        Resume resume = result.getResume();

        // 해당 후보의 매칭 상태 조회
        MatchingStatus matchingStatus = matchingRepository
                .findByProposalPositionIdAndResumeIdIn(proposalPosition.getId(), List.of(resume.getId()))
                .stream()
                .findFirst()
                .map(Matching::getStatus)
                .orElse(null);

        return new RecommendationResumeDetailViewModel(
                proposal.getId(),
                run.getId(),
                result.getId(),
                proposal.getTitle(),
                resolvePositionTitle(proposalPosition),
                "/proposals/%d/recommendations/results?runId=%d".formatted(proposal.getId(), run.getId()),
                toCandidateItem(result, matchingStatus),
                resume.getPortfolioUrl(),
                toSkillItems(resume.getSkills()),
                toCareerItems(resume)
        );
    }

    private Map<Long, MatchingStatus> loadMatchingStatusMap(Long positionId, Collection<Long> resumeIds) {
        if (resumeIds.isEmpty()) {
            return Map.of();
        }
        return matchingRepository.findByProposalPositionIdAndResumeIdIn(positionId, resumeIds)
                .stream()
                .collect(Collectors.toMap(
                        m -> m.getResume().getId(),
                        Matching::getStatus,
                        (a, b) -> a  // 동일 resume에 매칭이 여러 개면 첫 번째 유지
                ));
    }

    private RecommendationCandidateItem toCandidateItem(RecommendationResult result, MatchingStatus matchingStatus) {
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
                result.getLlmStatus() == LlmStatus.READY,
                matchingStatus != null ? matchingStatus.name() : null
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

    private static List<RecommendationResumeSkillItem> toSkillItems(SortedSet<ResumeSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }

        return skills.stream()
                .map(skill -> new RecommendationResumeSkillItem(
                        skill.getSkill().getName(),
                        skill.getProficiency().getDescription()
                ))
                .toList();
    }

    private static List<RecommendationResumeCareerItem> toCareerItems(Resume resume) {
        if (resume.getCareer() == null || resume.getCareer().getItems() == null || resume.getCareer().getItems().isEmpty()) {
            return List.of();
        }

        return resume.getCareer().getItems().stream()
                .map(item -> new RecommendationResumeCareerItem(
                        item.getCompanyName(),
                        item.getPosition(),
                        item.getEmploymentType() != null ? item.getEmploymentType().getDescription() : "-",
                        formatPeriod(item.getStartYearMonth(), item.getEndYearMonth(), item.getCurrentlyWorking()),
                        item.getSummary(),
                        item.getTechStack() == null ? List.of() : item.getTechStack()
                ))
                .toList();
    }

    private static String formatPeriod(String startYearMonth, String endYearMonth, Boolean currentlyWorking) {
        String start = (startYearMonth == null || startYearMonth.isBlank()) ? "-" : startYearMonth;
        if (currentlyWorking != null && currentlyWorking) {
            return start + " ~ " + "\uD604\uC7AC";
        }

        String end = (endYearMonth == null || endYearMonth.isBlank()) ? "-" : endYearMonth;
        return start + " ~ " + end;
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
