package com.generic4.itda.domain.matching;

import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.shared.BaseTimeEntity;
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
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Matching extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_position_id", nullable = false)
    private ProposalPosition proposalPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_member_id", nullable = false)
    private Member clientMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_member_id", nullable = false)
    private Member freelancerMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingStatus status;

    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime contractDate;
    private LocalDateTime completeDate;

    @Builder
    private Matching(
            Resume resume,
            ProposalPosition proposalPosition,
            Member clientMember,
            Member freelancerMember,
            MatchingStatus status,
            LocalDateTime requestedAt,
            LocalDateTime acceptedAt,
            LocalDateTime rejectedAt,
            LocalDateTime canceledAt,
            LocalDateTime contractDate,
            LocalDateTime completeDate
    ) {
        this.resume = resume;
        this.proposalPosition = proposalPosition;
        this.clientMember = clientMember;
        this.freelancerMember = freelancerMember;
        this.status = status != null ? status : MatchingStatus.PROPOSED;
        this.requestedAt = requestedAt;
        this.acceptedAt = acceptedAt;
        this.rejectedAt = rejectedAt;
        this.canceledAt = canceledAt;
        this.contractDate = contractDate;
        this.completeDate = completeDate;
    }

    public static Matching create(
            Resume resume,
            ProposalPosition proposalPosition,
            Member clientMember,
            Member freelancerMember
    ) {
        return Matching.builder()
                .resume(resume)
                .proposalPosition(proposalPosition)
                .clientMember(clientMember)
                .freelancerMember(freelancerMember)
                .status(MatchingStatus.PROPOSED)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    public void accept() {
        if (this.status != MatchingStatus.PROPOSED) {
            throw new IllegalStateException("제안 상태의 매칭만 수락할 수 있습니다.");
        }
        this.status = MatchingStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public void reject() {
        if (this.status != MatchingStatus.PROPOSED) {
            throw new IllegalStateException("제안 상태의 매칭만 거절할 수 있습니다.");
        }
        this.status = MatchingStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
    }
}