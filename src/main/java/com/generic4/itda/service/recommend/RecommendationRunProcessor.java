package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import com.generic4.itda.service.recommend.RecommendationCandidate.CandidateSkill;
import com.generic4.itda.service.recommend.scoring.HeuristicV1RecommendationScorer;
import com.generic4.itda.service.recommend.scoring.model.RecommendationScorableCandidate;
import com.generic4.itda.service.recommend.scoring.model.ScoredCandidate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RecommendationRunProcessor {

    private final RecommendationRunRepository recommendationRunRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final RecommendationCandidateFinder recommendationCandidateFinder;
    private final HeuristicV1RecommendationScorer recommendationScorer;
    private final RecommendationResultCreator recommendationResultCreator;

    public void process(Long runId) {
        RecommendationRun run = recommendationRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 추천 실행입니다."));

        if (!run.isRunning()) {
            throw new IllegalStateException("RUNNING 상태의 추천 실행만 처리할 수 있습니다.");
        }

        try {
            processRunningRun(run);
            log.info("Recommendation run completed. runId={}", runId);
        } catch (Exception e) {
            log.error("Recommendation run failed. runId={}", runId, e);
            run.markFailed(resolveErrorMessage(e));
        }
    }

    private void processRunningRun(RecommendationRun run) {
        ProposalPosition proposalPosition = run.getProposalPosition();

        List<RecommendationCandidate> candidates = recommendationCandidateFinder.findCandidates(
                proposalPosition,
                run.getTopK()
        );

        Set<String> requiredSkillNames = extractRequiredSkillNames(proposalPosition);
        Set<String> preferredSkillNames = extractPreferredSkillNames(proposalPosition);

        List<RecommendationScorableCandidate> scorableCandidates = candidates.stream()
                .map(this::toScorableCandidate)
                .toList();

        List<ScoredCandidate> scoredCandidates = recommendationScorer.score(
                proposalPosition.getProposal(),
                proposalPosition,
                requiredSkillNames,
                preferredSkillNames,
                scorableCandidates
        );

        List<RecommendationResult> results = recommendationResultCreator.create(
                run,
                scoredCandidates,
                run.getTopK(),
                requiredSkillNames
        );

        recommendationResultRepository.saveAll(results);
        HardFilterStat hardFilterStat = new HardFilterStat(candidates.size(), results.size());
        run.markCompleted(hardFilterStat);
    }

    private String resolveErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return "추천 실행 중 오류가 발생했습니다.";
        }
        return message;
    }

    private Set<String> extractRequiredSkillNames(ProposalPosition proposalPosition) {
        return proposalPosition.getSkills().stream()
                .filter(pps -> pps.getImportance() == ProposalPositionSkillImportance.ESSENTIAL)
                .map(pps -> pps.getSkill().getName())
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> extractPreferredSkillNames(ProposalPosition proposalPosition) {
        return proposalPosition.getSkills().stream()
                .filter(pps -> pps.getImportance() == ProposalPositionSkillImportance.PREFERENCE)
                .map(pps -> pps.getSkill().getName())
                .collect(Collectors.toUnmodifiableSet());
    }

    private RecommendationScorableCandidate toScorableCandidate(RecommendationCandidate candidate) {
        return new RecommendationScorableCandidate(
                candidate.resumeId(),
                candidate.careerYears(),
                candidate.skills().stream()
                        .map(CandidateSkill::skillName)
                        .collect(Collectors.toSet())
        );
    }
}
