package com.generic4.itda.domain.proposal;

import com.generic4.itda.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.Assert;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = false)
@Table(
        name = "proposal_ai_interview_message",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_proposal_ai_interview_message_proposal_sequence",
                        columnNames = {"proposal_id", "sequence"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_proposal_ai_interview_message_proposal_sequence",
                        columnList = "proposal_id, sequence"
                )
        }
)
public class ProposalAiInterviewMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false, updatable = false)
    private Proposal proposal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiInterviewMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer sequence;

    @Builder(access = AccessLevel.PRIVATE)
    private ProposalAiInterviewMessage(
            Proposal proposal,
            AiInterviewMessageRole role,
            String content,
            Integer sequence
    ) {
        validateProposal(proposal);
        validateRole(role);
        validateContent(content);
        validateSequence(sequence);

        this.proposal = proposal;
        this.role = role;
        this.content = content.trim();
        this.sequence = sequence;
    }

    public static ProposalAiInterviewMessage createUserMessage(
            Proposal proposal,
            String content,
            Integer sequence
    ) {
        return ProposalAiInterviewMessage.builder()
                .proposal(proposal)
                .role(AiInterviewMessageRole.USER)
                .content(content)
                .sequence(sequence)
                .build();
    }

    public static ProposalAiInterviewMessage createAssistantMessage(
            Proposal proposal,
            String content,
            Integer sequence
    ) {
        return ProposalAiInterviewMessage.builder()
                .proposal(proposal)
                .role(AiInterviewMessageRole.ASSISTANT)
                .content(content)
                .sequence(sequence)
                .build();
    }

    public String toRawInputLine() {
        return "[" + role.name() + "] " + content;
    }

    private static void validateProposal(Proposal proposal) {
        Assert.notNull(proposal, "제안서는 필수값입니다.");
    }

    private static void validateRole(AiInterviewMessageRole role) {
        Assert.notNull(role, "AI 인터뷰 메시지 역할은 필수값입니다.");
    }

    private static void validateContent(String content) {
        Assert.hasText(content, "AI 인터뷰 메시지 내용은 필수값입니다.");
    }

    private static void validateSequence(Integer sequence) {
        Assert.notNull(sequence, "AI 인터뷰 메시지 순서는 필수값입니다.");
        Assert.isTrue(sequence > 0, "AI 인터뷰 메시지 순서는 양수여야 합니다.");
    }
}