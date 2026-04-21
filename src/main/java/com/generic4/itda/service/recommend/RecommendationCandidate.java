package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.ResumeSkill;
import com.generic4.itda.domain.resume.ResumeStatus;
import java.util.List;
import java.util.Objects;

public record RecommendationCandidate(
        Long resumeId,
        ResumeStatus resumeStatus,
        boolean publiclyVisible,
        boolean aiMatchingEnabled,
        byte careerYears,
        List<CandidateSkill> skills
) {

    public RecommendationCandidate {
        Objects.requireNonNull(resumeId);
        Objects.requireNonNull(resumeStatus);
        Objects.requireNonNull(skills);
        skills = List.copyOf(skills); //방어적 복사
    }

    public boolean isActive() {
        return resumeStatus == ResumeStatus.ACTIVE;
    }

    public record CandidateSkill(
            Long skillId,
            String skillName,
            Proficiency proficiency
    ) {

        public CandidateSkill {
            Objects.requireNonNull(skillId);
            Objects.requireNonNull(skillName);
            Objects.requireNonNull(proficiency);
        }

        public static CandidateSkill of(ResumeSkill resumeSkill) {
            Objects.requireNonNull(resumeSkill);
            Objects.requireNonNull(resumeSkill.getSkill());

            return new CandidateSkill(
                    resumeSkill.getSkill().getId(),
                    resumeSkill.getSkill().getName(),
                    resumeSkill.getProficiency()
            );
        }
    }
}
