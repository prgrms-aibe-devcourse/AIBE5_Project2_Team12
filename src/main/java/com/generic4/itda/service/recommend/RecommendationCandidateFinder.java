package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.repository.ResumeQueryRepository;
import com.generic4.itda.repository.ResumeRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationCandidateFinder {

    private static final int MINIMUM_CANDIDATE_POOL_SIZE = 100;

    private final ResumeQueryRepository resumeQueryRepository;
    private final ResumeRepository resumeRepository;

    public List<RecommendationCandidate> findCandidates(ProposalPosition proposalPosition, int topK) {
        List<Long> requiredSkillIds = extractRequiredSkillIds(proposalPosition);
        int candidatePoolSize = resolveCandidatePoolSize(topK);

        List<Long> candidateResumeIds;
        if (requiredSkillIds.isEmpty()) {
            candidateResumeIds = resumeQueryRepository.findRecommendableResumeIds(proposalPosition, candidatePoolSize);
        } else {
            candidateResumeIds = resumeQueryRepository.findCandidatePool(
                            proposalPosition,
                            requiredSkillIds,
                            candidatePoolSize
                    ).stream()
                    .map(CandidatePoolRow::resumeId)
                    .toList();
        }

        if (candidateResumeIds.isEmpty()) {
            return List.of();
        }

        List<Resume> resumes = resumeRepository.findAllWithSkillsByIds(candidateResumeIds);
        Map<Long, Resume> resumeMap = resumes.stream()
                .collect(Collectors.toMap(Resume::getId, Function.identity()));

        return candidateResumeIds.stream()
                .map(resumeMap::get)
                .filter(Objects::nonNull)
                .filter(resume -> passesCareerRange(resume, proposalPosition))
                .map(this::toCandidate)
                .toList();
    }

    private List<Long> extractRequiredSkillIds(ProposalPosition proposalPosition) {
        return proposalPosition.getSkills().stream()
                .filter(pps -> pps.getImportance() == ProposalPositionSkillImportance.ESSENTIAL)
                .map(pps -> pps.getSkill().getId())
                .toList();
    }

    private int resolveCandidatePoolSize(int topK) {
        return Math.max(MINIMUM_CANDIDATE_POOL_SIZE, topK * 20);
    }

    private boolean passesCareerRange(Resume resume, ProposalPosition proposalPosition) {
        Integer candidateCareerYears = Integer.valueOf(resume.getCareerYears());
        Integer careerMinYears = proposalPosition.getCareerMinYears();
        Integer careerMaxYears = proposalPosition.getCareerMaxYears();

        if (careerMinYears != null && candidateCareerYears < careerMinYears) {
            return false;
        }

        if (careerMaxYears != null && candidateCareerYears > careerMaxYears) {
            return false;
        }

        return true;
    }

    private RecommendationCandidate toCandidate(Resume resume) {
        return new RecommendationCandidate(
                resume.getId(),
                resume.getStatus(),
                resume.isPubliclyVisible(),
                resume.isAiMatchingEnabled(),
                resume.getCareerYears(),
                resume.getSkills().stream()
                        .map(RecommendationCandidate.CandidateSkill::of)
                        .toList()
        );
    }

}
