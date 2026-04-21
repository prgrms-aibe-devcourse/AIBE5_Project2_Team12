package com.generic4.itda.repository;

import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.QResume;
import com.generic4.itda.domain.resume.QResumeSkill;
import com.generic4.itda.domain.resume.ResumeStatus;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.service.recommend.CandidatePoolRow;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResumeQueryRepositoryImpl implements ResumeQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QResume resume = QResume.resume;
    private static final QResumeSkill resumeSkill = QResumeSkill.resumeSkill;

    @Override
    public List<CandidatePoolRow> findCandidatePool(
            ProposalPosition proposalPosition,
            List<Long> requiredSkillIds,
            int candidatePoolSize
    ) {
        NumberExpression<Integer> proficiencySquaredSum = new CaseBuilder()
                .when(resumeSkill.proficiency.eq(Proficiency.BEGINNER)).then(1)
                .when(resumeSkill.proficiency.eq(Proficiency.INTERMEDIATE)).then(4)
                .when(resumeSkill.proficiency.eq(Proficiency.ADVANCED)).then(9)
                .otherwise(0)
                .sum();

        NumberExpression<Long> matchedRequiredSkillCount = resumeSkill.skill.id.countDistinct();

        return queryFactory
                .select(Projections.constructor(
                        CandidatePoolRow.class,
                        resume.id,
                        matchedRequiredSkillCount,
                        proficiencySquaredSum,
                        resume.careerYears
                ))
                .from(resume)
                .join(resume.skills, resumeSkill)
                .where(
                        recommendable(),
                        workTypeMatches(proposalPosition),
                        excludeProposalOwner(proposalPosition),
                        resumeSkill.skill.id.in(requiredSkillIds)
                )
                .groupBy(resume.id, resume.careerYears)
                .orderBy(
                        matchedRequiredSkillCount.desc(),
                        proficiencySquaredSum.desc(),
                        resume.careerYears.desc(),
                        resume.id.asc()
                )
                .limit(candidatePoolSize)
                .fetch();
    }

    @Override
    public List<Long> findRecommendableResumeIds(
            ProposalPosition proposalPosition,
            int candidatePoolSize
    ) {
        return queryFactory
                .select(resume.id)
                .from(resume)
                .where(
                        recommendable(),
                        workTypeMatches(proposalPosition),
                        excludeProposalOwner(proposalPosition)
                )
                .orderBy(
                        resume.careerYears.desc(),
                        resume.id.asc()
                )
                .limit(candidatePoolSize)
                .fetch();
    }

    private BooleanExpression recommendable() {
        return resume.status.eq(ResumeStatus.ACTIVE)
                .and(resume.publiclyVisible.isTrue())
                .and(resume.aiMatchingEnabled.isTrue())
                .and(resume.writingStatus.eq(ResumeWritingStatus.DONE));
    }

    private BooleanExpression workTypeMatches(ProposalPosition proposalPosition) {
        ProposalWorkType workType = proposalPosition.getWorkType();

        if (workType == null) {
            return null;
        }

        return switch (workType) {
            case SITE -> resume.preferredWorkType.in(WorkType.SITE, WorkType.HYBRID);
            case REMOTE -> resume.preferredWorkType.in(WorkType.REMOTE, WorkType.HYBRID);
            case HYBRID -> resume.preferredWorkType.eq(WorkType.HYBRID);
        };
    }

    private BooleanExpression excludeProposalOwner(ProposalPosition proposalPosition) {
        Long proposalOwnerId = proposalPosition.getProposal().getMember().getId();
        return resume.member.id.ne(proposalOwnerId);
    }
}
