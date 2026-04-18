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
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Entity
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

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    private ProposalWorkType workType;

    private Long headCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalPositionStatus status;

    private Long unitBudgetMin;

    private Long unitBudgetMax;

    private Long expectedPeriod;

    private Integer careerMinYears;

    private Integer careerMaxYears;

    @Column(length = 255)
    private String workPlace;

    @OneToMany(mappedBy = "proposalPosition", fetch = FetchType.LAZY, cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<ProposalPositionSkill> skills = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private ProposalPosition(Proposal proposal, Position position, String title, ProposalWorkType workType,
            Long headCount, ProposalPositionStatus status, Long unitBudgetMin, Long unitBudgetMax,
            Long expectedPeriod, Integer careerMinYears, Integer careerMaxYears, String workPlace) {
        Assert.notNull(proposal, "상위 제안서는 필수값입니다.");
        Assert.notNull(position, "직무는 필수값입니다.");
        validateTitle(title);
        validateHeadCount(headCount);
        validateBudgetRange(unitBudgetMin, unitBudgetMax);
        validateExpectedPeriod(expectedPeriod);
        validateCareerRange(careerMinYears, careerMaxYears);

        this.proposal = proposal;
        this.position = position;
        this.title = title.trim();
        this.workType = workType;
        this.headCount = headCount;
        this.status = status == null ? ProposalPositionStatus.OPEN : status;
        this.unitBudgetMin = unitBudgetMin;
        this.unitBudgetMax = unitBudgetMax;
        this.expectedPeriod = expectedPeriod;
        this.careerMinYears = careerMinYears;
        this.careerMaxYears = careerMaxYears;
        this.workPlace = normalizeOptionalShortText(workPlace);
    }

    public static ProposalPosition create(Proposal proposal, Position position, String title, ProposalWorkType workType,
            Long headCount, Long unitBudgetMin, Long unitBudgetMax, Long expectedPeriod, Integer careerMinYears,
            Integer careerMaxYears, String workPlace) {
        return ProposalPosition.builder()
                .proposal(proposal)
                .position(position)
                .title(title)
                .workType(workType)
                .headCount(headCount)
                .status(ProposalPositionStatus.OPEN)
                .unitBudgetMin(unitBudgetMin)
                .unitBudgetMax(unitBudgetMax)
                .expectedPeriod(expectedPeriod)
                .careerMinYears(careerMinYears)
                .careerMaxYears(careerMaxYears)
                .workPlace(workPlace)
                .build();
    }

    public static ProposalPosition create(Proposal proposal, Position position, Long headCount,
            Long unitBudgetMin, Long unitBudgetMax) {
        return create(proposal, position, position.getName(), null, headCount, unitBudgetMin, unitBudgetMax,
                null, null, null, null);
    }

    public void update(Position position, String title, ProposalWorkType workType, Long headCount, Long unitBudgetMin,
            Long unitBudgetMax, Long expectedPeriod, Integer careerMinYears, Integer careerMaxYears,
            String workPlace) {
        Assert.notNull(position, "직무는 필수값입니다.");
        validateTitle(title);
        validateHeadCount(headCount);
        validateBudgetRange(unitBudgetMin, unitBudgetMax);
        validateExpectedPeriod(expectedPeriod);
        validateCareerRange(careerMinYears, careerMaxYears);
        this.proposal.validatePositionChange(this, position);

        this.position = position;
        this.title = title.trim();
        this.workType = workType;
        this.headCount = headCount;
        this.unitBudgetMin = unitBudgetMin;
        this.unitBudgetMax = unitBudgetMax;
        this.expectedPeriod = expectedPeriod;
        this.careerMinYears = careerMinYears;
        this.careerMaxYears = careerMaxYears;
        this.workPlace = normalizeOptionalShortText(workPlace);
    }

    public void update(Position position, Long headCount, Long unitBudgetMin, Long unitBudgetMax) {
        update(position, position.getName(), null, headCount, unitBudgetMin, unitBudgetMax, null, null, null, null);
    }

    public void changeStatus(ProposalPositionStatus status) {
        Assert.notNull(status, "모집 상태는 필수값입니다.");
        this.status = status;
    }

    public ProposalPositionSkill addSkill(Skill skill, ProposalPositionSkillImportance importance) {
        ProposalPositionSkill proposalPositionSkill = ProposalPositionSkill.create(this, skill, importance);
        validateSkillChange(proposalPositionSkill.getSkill());
        this.skills.add(proposalPositionSkill);
        return proposalPositionSkill;
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

    void validateSkillChange(Skill skill) {
        Assert.notNull(skill, "스킬은 필수값입니다.");
        Assert.state(isSkillNotDuplicated(skill), "같은 모집 단위에는 동일한 스킬을 중복 등록할 수 없습니다.");
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

    private static void validateTitle(String title) {
        Assert.hasText(title, "포지션 제목은 필수값입니다.");
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

    private static void validateExpectedPeriod(Long expectedPeriod) {
        if (expectedPeriod != null) {
            Assert.isTrue(expectedPeriod > 0, "포지션 예상 기간은 양수여야 합니다.");
        }
    }

    private static void validateCareerRange(Integer careerMinYears, Integer careerMaxYears) {
        if (careerMinYears != null) {
            Assert.isTrue(careerMinYears >= 0, "최소 경력 연차는 0 이상이어야 합니다.");
        }
        if (careerMaxYears != null) {
            Assert.isTrue(careerMaxYears >= 0, "최대 경력 연차는 0 이상이어야 합니다.");
        }
        if (careerMinYears != null && careerMaxYears != null) {
            Assert.isTrue(careerMinYears <= careerMaxYears, "최소 경력 연차는 최대 경력 연차보다 클 수 없습니다.");
        }
    }

    private static String normalizeOptionalShortText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
