package com.generic4.itda.repository;

import com.generic4.itda.service.recommend.CandidatePoolRow;
import java.util.List;

public interface ResumeQueryRepository {

    List<CandidatePoolRow> findCandidatePool(List<Long> requiredSkillIds, int candidatePoolSize);

    List<Long> findRecommendableResumeIds(int candidatePoolSize);
}
