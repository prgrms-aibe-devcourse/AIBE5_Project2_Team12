package com.generic4.itda.domain.matching;

import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.shared.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Matching extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    private LocalDateTime contractDate;
    private LocalDateTime completeDate;

    @Builder
    private Matching(Resume resume, ProposalPosition proposalPosition, Member clientMember,
                     Member freelancerMember, MatchingStatus status) {
        this.resume = resume;
        this.proposalPosition = proposalPosition;
        this.clientMember = clientMember;
        this.freelancerMember = freelancerMember;
        this.status = status != null ? status : MatchingStatus.PROPOSED;
    }

    public static Matching create(Resume resume, ProposalPosition proposalPosition, Member clientMember, Member freelancerMember) {
        return Matching.builder()
                .resume(resume)
                .proposalPosition(proposalPosition)
                .clientMember(clientMember)
                .freelancerMember(freelancerMember)
                .status(MatchingStatus.PROPOSED)
                .build();
    }

    public void accept() {
        if (this.status != MatchingStatus.PROPOSED) {
            throw new IllegalStateException("제안 상태의 매칭만 수락할 수 있습니다.");
        }
        this.status = MatchingStatus.ACCEPTED;
    }

    public void reject() {
        if (this.status != MatchingStatus.PROPOSED) {
            throw new IllegalStateException("제안 상태의 매칭만 거절할 수 있습니다.");
        }
        this.status = MatchingStatus.REJECTED;
    }
}
