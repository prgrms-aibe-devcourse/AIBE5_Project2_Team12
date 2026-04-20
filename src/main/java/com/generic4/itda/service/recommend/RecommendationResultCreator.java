package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.repository.ResumeRepository;
import com.generic4.itda.service.recommend.scoring.model.ScoredCandidate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationResultCreator {

    private static final int SCORE_SCALE = 4;
    private final ResumeRepository resumeRepository;

    List<RecommendationResult> create(
            RecommendationRun run,
            List<ScoredCandidate> scoredCandidates,
            int topK,
            Set<String> requiredSkillNames
    ) {
        List<ScoredCandidate> topCandidates = selectTopCandidates(scoredCandidates, topK);
        if (topCandidates.isEmpty()) {
            return List.of();
        }

        Map<Long, Resume> resumeMap = loadResumeMap(topCandidates);

        List<RecommendationResult> results = new ArrayList<>();
        int rank = 1;

        for (ScoredCandidate candidate : topCandidates) {
            results.add(
                    RecommendationResult.create(
                            run,
                            resumeMap.get(candidate.resumeId()),
                            rank++,
                            toBigDecimal(candidate.finalScore()),
                            toBigDecimal(candidate.similarityScore()),
                            createReasonFacts(candidate, requiredSkillNames)
                    )
            );
        }

        return results;
    }

    private List<ScoredCandidate> selectTopCandidates(List<ScoredCandidate> scoredCandidates, int topK) {
        if (scoredCandidates == null || scoredCandidates.isEmpty()) {
            return List.of();
        }

        List<ScoredCandidate> deduplicated = new ArrayList<>(
                scoredCandidates.stream()
                        .sorted(resultComparator())
                        .collect(Collectors.toMap(
                                ScoredCandidate::resumeId,
                                Function.identity(),
                                (first, second) -> first,
                                LinkedHashMap::new
                        ))
                        .values()
        );

        return deduplicated.stream()
                .limit(topK)
                .toList();
    }

    private Comparator<ScoredCandidate> resultComparator() {
        return Comparator.comparing(ScoredCandidate::finalScore).reversed()
                .thenComparing(ScoredCandidate::candidateId);
    }

    private Map<Long, Resume> loadResumeMap(List<ScoredCandidate> topCandidates) {
        List<Long> resumeIds = topCandidates.stream()
                .map(ScoredCandidate::resumeId)
                .toList();

        Map<Long, Resume> resumeMap = resumeRepository.findAllById(resumeIds).stream()
                .collect(Collectors.toMap(
                        Resume::getId,
                        Function.identity()
                ));

        if (resumeMap.size() != resumeIds.size()) {
            throw new IllegalStateException("추천 결과 생성 대상 이력서 일부를 찾을 수 없습니다.");
        }

        return resumeMap;
    }

    private BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private ReasonFacts createReasonFacts(
            ScoredCandidate candidate,
            Set<String> requiredSkillNames
    ) {
        List<String> matchedSkills = extractMatchedSkills(candidate, requiredSkillNames);

        return new ReasonFacts(
                matchedSkills,
                List.of(), // matchedDomains (placeholder)
                candidate.careerYears(),
                createHighlights(candidate, matchedSkills)
        );
    }

    private List<String> extractMatchedSkills(
            ScoredCandidate candidate,
            Set<String> requiredSkillNames
    ) {
        if (requiredSkillNames == null || requiredSkillNames.isEmpty()) {
            return List.of();
        }
        return candidate.ownedSkillNames().stream()
                .filter(requiredSkillNames::contains)
                .sorted()
                .toList();
    }

    private List<String> createHighlights(
            ScoredCandidate candidate,
            List<String> matchedSkills
    ) {
        List<String> highlights = new ArrayList<>();

        if (!matchedSkills.isEmpty()) {
            highlights.add("공토 스킬 " + matchedSkills.size() + "개 보유");
        }

        if (candidate.careerYears() > 0) {
            highlights.add("관련 경력 " + candidate.careerYears() + "년");
        }

        if (candidate.similarityScore() >= 0.8) {
            highlights.add("요구 조건과 높은 유사도");
        }

        return highlights;
    }
}
