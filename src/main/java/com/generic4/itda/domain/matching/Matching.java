package com.generic4.itda.domain.matching;

import com.generic4.itda.domain.matching.constant.MatchingCancellationPhase;
import com.generic4.itda.domain.matching.constant.MatchingCancellationReason;
import com.generic4.itda.domain.matching.constant.MatchingParticipantRole;
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
    private LocalDateTime clientContractAcceptedAt;
    private LocalDateTime freelancerContractAcceptedAt;

    @Enumerated(EnumType.STRING)
    private MatchingParticipantRole cancellationRequestedBy;

    @Enumerated(EnumType.STRING)
    private MatchingCancellationReason cancellationReason;

    @Column(length = 500)
    private String cancellationReasonDetail;

    private LocalDateTime cancellationRequestedAt;
    private LocalDateTime cancellationAutoCancelAt;

    @Column(columnDefinition = "text")
    private String clientReview;

    private LocalDateTime clientReviewedAt;

    @Column(columnDefinition = "text")
    private String freelancerReview;

    private LocalDateTime freelancerReviewedAt;
    private LocalDateTime clientCompletionConfirmedAt;
    private LocalDateTime freelancerCompletionConfirmedAt;

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
            LocalDateTime completeDate,
            LocalDateTime clientContractAcceptedAt,
            LocalDateTime freelancerContractAcceptedAt,
            MatchingParticipantRole cancellationRequestedBy,
            MatchingCancellationReason cancellationReason,
            String cancellationReasonDetail,
            LocalDateTime cancellationRequestedAt,
            LocalDateTime cancellationAutoCancelAt,
            String clientReview,
            LocalDateTime clientReviewedAt,
            String freelancerReview,
            LocalDateTime freelancerReviewedAt,
            LocalDateTime clientCompletionConfirmedAt,
            LocalDateTime freelancerCompletionConfirmedAt
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
        this.clientContractAcceptedAt = clientContractAcceptedAt;
        this.freelancerContractAcceptedAt = freelancerContractAcceptedAt;
        this.cancellationRequestedBy = cancellationRequestedBy;
        this.cancellationReason = cancellationReason;
        this.cancellationReasonDetail = cancellationReasonDetail;
        this.cancellationRequestedAt = cancellationRequestedAt;
        this.cancellationAutoCancelAt = cancellationAutoCancelAt;
        this.clientReview = clientReview;
        this.clientReviewedAt = clientReviewedAt;
        this.freelancerReview = freelancerReview;
        this.freelancerReviewedAt = freelancerReviewedAt;
        this.clientCompletionConfirmedAt = clientCompletionConfirmedAt;
        this.freelancerCompletionConfirmedAt = freelancerCompletionConfirmedAt;
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

    public void acceptContractStart(MatchingParticipantRole participantRole) {
        if (this.status != MatchingStatus.ACCEPTED) {
            throw new IllegalStateException("수락된 매칭만 계약 시작을 수락할 수 있습니다.");
        }
        if (hasCancellationRequest()) {
            throw new IllegalStateException("취소 요청 중인 매칭은 계약 시작을 수락할 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (participantRole == MatchingParticipantRole.CLIENT && this.clientContractAcceptedAt == null) {
            this.clientContractAcceptedAt = now;
        }
        if (participantRole == MatchingParticipantRole.FREELANCER && this.freelancerContractAcceptedAt == null) {
            this.freelancerContractAcceptedAt = now;
        }

        if (this.clientContractAcceptedAt != null && this.freelancerContractAcceptedAt != null) {
            this.status = MatchingStatus.IN_PROGRESS;
            this.contractDate = now;
        }
    }

    public void requestCancellation(
            MatchingParticipantRole requesterRole,
            MatchingCancellationReason reason,
            String reasonDetail
    ) {
        if (this.status != MatchingStatus.ACCEPTED && this.status != MatchingStatus.IN_PROGRESS) {
            throw new IllegalStateException("수락 또는 진행 중인 매칭만 취소를 요청할 수 있습니다.");
        }
        if (hasCancellationRequest()) {
            throw new IllegalStateException("이미 취소 요청이 진행 중입니다.");
        }

        MatchingCancellationPhase phase = resolveCancellationPhase();
        validateCancellationReason(requesterRole, reason, reasonDetail, phase);

        LocalDateTime now = LocalDateTime.now();
        this.cancellationRequestedBy = requesterRole;
        this.cancellationReason = reason;
        this.cancellationReasonDetail = reason.isOther() ? normalize(reasonDetail) : null;
        this.cancellationRequestedAt = now;
        this.cancellationAutoCancelAt = phase == MatchingCancellationPhase.BEFORE_CONTRACT
                ? now.plusHours(24)
                : null;
    }

    public void withdrawCancellation(MatchingParticipantRole requesterRole) {
        if (!hasCancellationRequest()) {
            throw new IllegalStateException("진행 중인 취소 요청이 없습니다.");
        }
        if (this.cancellationRequestedBy != requesterRole) {
            throw new IllegalStateException("취소 요청자만 취소 요청을 철회할 수 있습니다.");
        }
        clearCancellationRequest();
    }

    public void confirmCancellation(MatchingParticipantRole confirmerRole) {
        if (!hasCancellationRequest()) {
            throw new IllegalStateException("진행 중인 취소 요청이 없습니다.");
        }
        if (this.cancellationRequestedBy == confirmerRole) {
            throw new IllegalStateException("취소 요청자는 취소 확인을 할 수 없습니다.");
        }
        cancelNow();
    }

    public boolean cancelAutomaticallyIfOverdue(LocalDateTime now) {
        if (!hasCancellationRequest() || this.status != MatchingStatus.ACCEPTED) {
            return false;
        }
        if (this.cancellationAutoCancelAt == null || now.isBefore(this.cancellationAutoCancelAt)) {
            return false;
        }
        cancelNow();
        return true;
    }

    public void submitReview(MatchingParticipantRole reviewerRole, String review) {
        if (this.status != MatchingStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 매칭에서만 후기를 작성할 수 있습니다.");
        }
        if (hasCancellationRequest()) {
            throw new IllegalStateException("취소 요청 중인 매칭은 후기를 작성할 수 없습니다.");
        }
        String normalizedReview = normalize(review);
        if (normalizedReview == null) {
            throw new IllegalArgumentException("후기 내용은 필수입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (reviewerRole == MatchingParticipantRole.CLIENT) {
            this.clientReview = normalizedReview;
            this.clientReviewedAt = now;
        } else {
            this.freelancerReview = normalizedReview;
            this.freelancerReviewedAt = now;
        }
    }

    public void confirmCompletion(MatchingParticipantRole participantRole) {
        if (this.status != MatchingStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 매칭만 완료 확인을 할 수 있습니다.");
        }
        if (hasCancellationRequest()) {
            throw new IllegalStateException("취소 요청 중인 매칭은 완료 확인을 할 수 없습니다.");
        }
        if (!hasReviewBy(participantRole)) {
            throw new IllegalStateException("후기 작성 후 프로젝트 완료를 확인할 수 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (participantRole == MatchingParticipantRole.CLIENT) {
            this.clientCompletionConfirmedAt = now;
        } else {
            this.freelancerCompletionConfirmedAt = now;
        }

        if (this.clientCompletionConfirmedAt != null && this.freelancerCompletionConfirmedAt != null) {
            this.status = MatchingStatus.COMPLETED;
            this.completeDate = now;
        }
    }

    public boolean isContractStartAcceptedBy(MatchingParticipantRole participantRole) {
        return participantRole == MatchingParticipantRole.CLIENT
                ? this.clientContractAcceptedAt != null
                : this.freelancerContractAcceptedAt != null;
    }

    public boolean hasCancellationRequest() {
        return this.cancellationRequestedBy != null
                && (this.status == MatchingStatus.ACCEPTED || this.status == MatchingStatus.IN_PROGRESS);
    }

    public boolean hasReviewBy(MatchingParticipantRole participantRole) {
        return participantRole == MatchingParticipantRole.CLIENT
                ? this.clientReviewedAt != null
                : this.freelancerReviewedAt != null;
    }

    public boolean isCompletionConfirmedBy(MatchingParticipantRole participantRole) {
        return participantRole == MatchingParticipantRole.CLIENT
                ? this.clientCompletionConfirmedAt != null
                : this.freelancerCompletionConfirmedAt != null;
    }

    private MatchingCancellationPhase resolveCancellationPhase() {
        return this.status == MatchingStatus.ACCEPTED
                ? MatchingCancellationPhase.BEFORE_CONTRACT
                : MatchingCancellationPhase.AFTER_CONTRACT;
    }

    private void validateCancellationReason(
            MatchingParticipantRole requesterRole,
            MatchingCancellationReason reason,
            String reasonDetail,
            MatchingCancellationPhase phase
    ) {
        if (reason == null) {
            throw new IllegalArgumentException("취소 사유는 필수입니다.");
        }
        if (!reason.matches(phase, requesterRole)) {
            throw new IllegalArgumentException("현재 취소 상황에 맞지 않는 사유입니다.");
        }
        if (reason.isOther() && normalize(reasonDetail) == null) {
            throw new IllegalArgumentException("기타 취소 사유를 입력해주세요.");
        }
    }

    private void cancelNow() {
        this.status = MatchingStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    private void clearCancellationRequest() {
        this.cancellationRequestedBy = null;
        this.cancellationReason = null;
        this.cancellationReasonDetail = null;
        this.cancellationRequestedAt = null;
        this.cancellationAutoCancelAt = null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
