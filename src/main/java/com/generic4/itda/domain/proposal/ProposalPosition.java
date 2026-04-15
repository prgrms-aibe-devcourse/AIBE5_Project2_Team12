package com.generic4.itda.domain.proposal;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.shared.BaseEntity;
import com.generic4.itda.domain.skill.Skill;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.Assert;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_proposal_position_proposal_id_and_position_id",
                        columnNames = {"proposal_id", "position_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = false)
public class ProposalPosition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    private Long headCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalPositionStatus status;

    private Long unitBudgetMin;

    private Long unitBudgetMax;

    @OneToMany(mappedBy = "proposalPosition", fetch = FetchType.LAZY, cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<ProposalPositionSkill> skills = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private ProposalPosition(Proposal proposal, Position position, Long headCount, ProposalPositionStatus status,
            Long unitBudgetMin, Long unitBudgetMax) {
        Assert.notNull(proposal, "상위 제안서는 필수값입니다.");
        Assert.notNull(position, "직무는 필수값입니다.");
        validateHeadCount(headCount);
        validateBudgetRange(unitBudgetMin, unitBudgetMax);

        this.proposal = proposal;
        this.position = position;
        this.headCount = headCount;
        this.status = status == null ? ProposalPositionStatus.OPEN : status;
        this.unitBudgetMin = unitBudgetMin;
        this.unitBudgetMax = unitBudgetMax;
    }

    public static ProposalPosition create(Proposal proposal, Position position, Long headCount,
            Long unitBudgetMin, Long unitBudgetMax) {
        ProposalPosition proposalPosition = ProposalPosition.builder()
                .proposal(proposal)
                .position(position)
                .headCount(headCount)
                .status(ProposalPositionStatus.OPEN)
                .unitBudgetMin(unitBudgetMin)
                .unitBudgetMax(unitBudgetMax)
                .build();

        proposal.registerPosition(proposalPosition);
        return proposalPosition;
    }

    public void update(Position position, Long headCount, Long unitBudgetMin, Long unitBudgetMax) {
        Assert.notNull(position, "직무는 필수값입니다.");
        validateHeadCount(headCount);
        validateBudgetRange(unitBudgetMin, unitBudgetMax);
        this.proposal.validatePositionChange(this, position);

        this.position = position;
        this.headCount = headCount;
        this.unitBudgetMin = unitBudgetMin;
        this.unitBudgetMax = unitBudgetMax;
    }

    public void changeStatus(ProposalPositionStatus status) {
        Assert.notNull(status, "모집 상태는 필수값입니다.");
        this.status = status;
    }

    public ProposalPositionSkill addSkill(Skill skill, ProposalPositionSkillImportance importance) {
        return ProposalPositionSkill.create(this, skill, importance);
    }

    void registerSkill(ProposalPositionSkill proposalPositionSkill) {
        Assert.notNull(proposalPositionSkill, "요구 스킬은 필수값입니다.");
        Assert.state(proposalPositionSkill.getProposalPosition() == this,
                "해당 요구 스킬은 이 모집 단위에 속해야 합니다.");
        Assert.state(isSkillNotDuplicated(proposalPositionSkill.getSkill()),
                "같은 모집 단위에는 동일한 스킬을 중복 등록할 수 없습니다.");

        if (!this.skills.contains(proposalPositionSkill)) {
            this.skills.add(proposalPositionSkill);
        }
    }

    public void removeSkill(Skill skill) {
        Assert.notNull(skill, "삭제할 스킬은 필수값입니다.");

        this.skills.removeIf(existing -> {
            if (hasSameSkill(existing.getSkill(), skill)) {
                existing.detachFromProposalPosition();
                return true;
            }
            return false;
        });
    }

    void detachFromProposal() {
        this.proposal = null;
    }

    private boolean isSkillNotDuplicated(Skill skill) {
        return this.skills.stream()
                .map(ProposalPositionSkill::getSkill)
                .noneMatch(existing -> hasSameSkill(existing, skill));
    }

    private boolean hasSameSkill(Skill source, Skill target) {
        if (source == target) {
            return true;
        }
        if (source.getId() != null && target.getId() != null) {
            return source.getId().equals(target.getId());
        }
        return source.getName().equals(target.getName());
    }

    private static void validateHeadCount(Long headCount) {
        if (headCount != null) {
            Assert.isTrue(headCount > 0, "모집 인원은 양수여야 합니다.");
        }
    }

    private static void validateBudgetRange(Long budgetMin, Long budgetMax) {
        if (budgetMin != null) {
            Assert.isTrue(budgetMin >= 0, "1인 기준 최소 예산은 음수일 수 없습니다.");
        }
        if (budgetMax != null) {
            Assert.isTrue(budgetMax >= 0, "1인 기준 최대 예산은 음수일 수 없습니다.");
        }
        if (budgetMin != null && budgetMax != null) {
            Assert.isTrue(budgetMin <= budgetMax, "1인 기준 최소 예산은 최대 예산보다 클 수 없습니다.");
        }
    }
}
