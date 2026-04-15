package com.generic4.itda.domain.proposal;

import com.generic4.itda.domain.shared.BaseTimeEntity;
import com.generic4.itda.domain.skill.Skill;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_proposal_position_skill_proposal_position_id_and_skill_id",
                        columnNames = {"proposal_position_id", "skill_id"}
                )
        }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProposalPositionSkill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_position_id", nullable = false)
    private ProposalPosition proposalPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalPositionSkillImportance importance;

    private ProposalPositionSkill(ProposalPosition proposalPosition, Skill skill,
            ProposalPositionSkillImportance importance) {
        Assert.notNull(proposalPosition, "모집 단위는 필수값입니다.");
        Assert.notNull(skill, "스킬은 필수값입니다.");

        this.proposalPosition = proposalPosition;
        this.skill = skill;
        this.importance = normalizeImportance(importance);
    }

    public static ProposalPositionSkill create(ProposalPosition proposalPosition, Skill skill,
            ProposalPositionSkillImportance importance) {
        return new ProposalPositionSkill(proposalPosition, skill, importance);
    }

    public void changeImportance(ProposalPositionSkillImportance importance) {
        this.importance = normalizeImportance(importance);
    }

    void detachFromProposalPosition() {
        this.proposalPosition = null;
    }

    private ProposalPositionSkillImportance normalizeImportance(ProposalPositionSkillImportance importance) {
        return importance == null ? ProposalPositionSkillImportance.PREFERENCE : importance;
    }
}
