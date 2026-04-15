package com.generic4.itda.domain.proposal;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.member.UserStatus;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.shared.BaseEntity;
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
import jakarta.persistence.Lob;
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
public class Proposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawInputText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private Long totalBudgetMin;

    private Long totalBudgetMax;

    @Enumerated(EnumType.STRING)
    private ProposalWorkType workType;

    @Column(length = 255)
    private String workPlace;

    private Long expectedPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status;

    @OneToMany(mappedBy = "proposal", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<ProposalPosition> positions = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Proposal(Member member, String title, String rawInputText, String description,
            Long totalBudgetMin, Long totalBudgetMax, ProposalWorkType workType, String workPlace,
            Long expectedPeriod, ProposalStatus status) {
        validateMember(member);
        validateBudgetRange(totalBudgetMin, totalBudgetMax, "전체");
        validateExpectedPeriod(expectedPeriod);

        this.member = member;
        this.title = normalizeRequiredTitle(title);
        this.rawInputText = normalizeRequiredRawInputText(rawInputText);
        this.description = normalizeOptionalText(description);
        this.totalBudgetMin = totalBudgetMin;
        this.totalBudgetMax = totalBudgetMax;
        this.workType = workType;
        this.workPlace = normalizeOptionalShortText(workPlace);
        this.expectedPeriod = expectedPeriod;
        this.status = status == null ? ProposalStatus.WRITING : status;
    }

    public static Proposal create(Member member, String title, String rawInputText, String description,
            Long totalBudgetMin, Long totalBudgetMax, ProposalWorkType workType, String workPlace,
            Long expectedPeriod) {
        return Proposal.builder()
                .member(member)
                .title(title)
                .rawInputText(rawInputText)
                .description(description)
                .totalBudgetMin(totalBudgetMin)
                .totalBudgetMax(totalBudgetMax)
                .workType(workType)
                .workPlace(workPlace)
                .expectedPeriod(expectedPeriod)
                .status(ProposalStatus.WRITING)
                .build();
    }

    public void update(String title, String rawInputText, String description, Long totalBudgetMin,
            Long totalBudgetMax, ProposalWorkType workType, String workPlace, Long expectedPeriod) {
        validateBudgetRange(totalBudgetMin, totalBudgetMax, "전체");
        validateExpectedPeriod(expectedPeriod);

        this.title = normalizeRequiredTitle(title);
        this.rawInputText = normalizeRequiredRawInputText(rawInputText);
        this.description = normalizeOptionalText(description);
        this.totalBudgetMin = totalBudgetMin;
        this.totalBudgetMax = totalBudgetMax;
        this.workType = workType;
        this.workPlace = normalizeOptionalShortText(workPlace);
        this.expectedPeriod = expectedPeriod;
    }

    public void startMatching() {
        Assert.state(this.status == ProposalStatus.WRITING, "작성 중인 제안서만 매칭을 시작할 수 있습니다.");
        this.status = ProposalStatus.MATCHING;
    }

    public void complete() {
        Assert.state(this.status == ProposalStatus.MATCHING, "매칭 중인 제안서만 종료할 수 있습니다.");
        this.status = ProposalStatus.COMPLETE;
    }

    public ProposalPosition addPosition(Position position, Long headCount, Long unitBudgetMin, Long unitBudgetMax) {
        ProposalPosition proposalPosition = ProposalPosition.create(this, position, headCount, unitBudgetMin,
                unitBudgetMax);
        validatePositionChange(proposalPosition, proposalPosition.getPosition());
        this.positions.add(proposalPosition);
        return proposalPosition;
    }

    void validatePositionChange(ProposalPosition proposalPosition, Position position) {
        Assert.notNull(proposalPosition, "모집 단위는 필수값입니다.");
        Assert.notNull(position, "직무는 필수값입니다.");
        Assert.state(isPositionNotDuplicated(proposalPosition, position),
                "같은 제안서에는 동일한 직무를 중복 등록할 수 없습니다.");
    }

    public void removePosition(ProposalPosition proposalPosition) {
        Assert.notNull(proposalPosition, "삭제할 모집 단위는 필수값입니다.");

        boolean removed = this.positions.remove(proposalPosition);
        if (removed) {
            proposalPosition.detachFromProposal();
        }
    }

    private static void validateMember(Member member) {
        Assert.notNull(member, "클라이언트 회원은 필수값입니다.");
        Assert.isTrue(member.getStatus() == UserStatus.ACTIVE, "활성 회원만 제안서를 작성할 수 있습니다.");
    }

    private boolean isPositionNotDuplicated(ProposalPosition source, Position target) {
        return this.positions.stream()
                .filter(existing -> existing != source)
                .map(ProposalPosition::getPosition)
                .noneMatch(existing -> hasSamePosition(existing, target));
    }

    private boolean hasSamePosition(Position source, Position target) {
        if (source == target) {
            return true;
        }
        if (source.getId() != null && target.getId() != null) {
            return source.getId().equals(target.getId());
        }
        return source.getName().equals(target.getName());
    }

    private static void validateBudgetRange(Long budgetMin, Long budgetMax, String label) {
        if (budgetMin != null) {
            Assert.isTrue(budgetMin >= 0, label + " 최소 예산은 음수일 수 없습니다.");
        }
        if (budgetMax != null) {
            Assert.isTrue(budgetMax >= 0, label + " 최대 예산은 음수일 수 없습니다.");
        }
        if (budgetMin != null && budgetMax != null) {
            Assert.isTrue(budgetMin <= budgetMax, label + " 최소 예산은 최대 예산보다 클 수 없습니다.");
        }
    }

    private static void validateExpectedPeriod(Long expectedPeriod) {
        if (expectedPeriod != null) {
            Assert.isTrue(expectedPeriod > 0, "예상 기간은 양수여야 합니다.");
        }
    }

    private static String normalizeRequiredTitle(String title) {
        Assert.hasText(title, "제안서 제목은 필수값입니다.");
        return title.trim();
    }

    private static String normalizeRequiredRawInputText(String rawInputText) {
        Assert.hasText(rawInputText, "제안서 원본 입력은 필수값입니다.");
        return rawInputText;
    }

    private static String normalizeOptionalShortText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
