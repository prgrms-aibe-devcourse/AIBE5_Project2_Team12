package com.generic4.itda.domain.matching;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.matching.constant.MatchingCancellationReason;
import com.generic4.itda.domain.matching.constant.MatchingParticipantRole;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchingTest {

    @Test
    @DisplayName("ACCEPTED 매칭은 양측 계약 시작 수락이 모두 완료되면 IN_PROGRESS로 전환된다")
    void acceptContractStart_changesStatusWhenBothParticipantsAccepted() {
        Matching matching = acceptedMatching();

        matching.acceptContractStart(MatchingParticipantRole.CLIENT);

        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.ACCEPTED);
        assertThat(matching.isContractStartAcceptedBy(MatchingParticipantRole.CLIENT)).isTrue();
        assertThat(matching.isContractStartAcceptedBy(MatchingParticipantRole.FREELANCER)).isFalse();
        assertThat(matching.getContractDate()).isNull();

        matching.acceptContractStart(MatchingParticipantRole.FREELANCER);

        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.IN_PROGRESS);
        assertThat(matching.isContractStartAcceptedBy(MatchingParticipantRole.FREELANCER)).isTrue();
        assertThat(matching.getContractDate()).isNotNull();
    }

    @Test
    @DisplayName("ACCEPTED 상태의 취소 요청은 자동 취소 예정 시각을 가진다")
    void requestCancellation_setsAutoCancelDeadlineBeforeContract() {
        Matching matching = acceptedMatching();

        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_BUDGET_OR_SCHEDULE_CHANGED,
                null
        );

        assertThat(matching.hasCancellationRequest()).isTrue();
        assertThat(matching.getCancellationRequestedBy()).isEqualTo(MatchingParticipantRole.CLIENT);
        assertThat(matching.getCancellationReason())
                .isEqualTo(MatchingCancellationReason.CLIENT_BEFORE_BUDGET_OR_SCHEDULE_CHANGED);
        assertThat(matching.getCancellationRequestedAt()).isNotNull();
        assertThat(matching.getCancellationAutoCancelAt())
                .isEqualTo(matching.getCancellationRequestedAt().plusHours(24));
    }

    @Test
    @DisplayName("IN_PROGRESS 상태의 취소 요청은 자동 취소 예정 시각을 갖지 않는다")
    void requestCancellation_doesNotSetAutoCancelDeadlineAfterContract() {
        Matching matching = inProgressMatching();

        matching.requestCancellation(
                MatchingParticipantRole.FREELANCER,
                MatchingCancellationReason.FREELANCER_AFTER_SCOPE_CHANGED,
                null
        );

        assertThat(matching.hasCancellationRequest()).isTrue();
        assertThat(matching.getCancellationRequestedBy()).isEqualTo(MatchingParticipantRole.FREELANCER);
        assertThat(matching.getCancellationAutoCancelAt()).isNull();
    }

    @Test
    @DisplayName("기타 취소 사유를 선택하면 상세 사유를 입력해야 한다")
    void requestCancellation_requiresDetailWhenReasonIsOther() {
        Matching matching = acceptedMatching();

        assertThatThrownBy(() -> matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_OTHER,
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기타 취소 사유를 입력해주세요.");

        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_OTHER,
                "내부 의사결정이 지연되었습니다."
        );

        assertThat(matching.getCancellationReasonDetail()).isEqualTo("내부 의사결정이 지연되었습니다.");
    }

    @Test
    @DisplayName("취소 요청자는 취소 요청을 철회할 수 있다")
    void withdrawCancellation_clearsCancellationRequest() {
        Matching matching = acceptedMatching();
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );

        matching.withdrawCancellation(MatchingParticipantRole.CLIENT);

        assertThat(matching.hasCancellationRequest()).isFalse();
        assertThat(matching.getCancellationReason()).isNull();
        assertThat(matching.getCancellationRequestedAt()).isNull();
        assertThat(matching.getCancellationAutoCancelAt()).isNull();
    }

    @Test
    @DisplayName("취소 요청 수신자가 확인하면 CANCELED로 전환된다")
    void confirmCancellation_changesStatusToCanceled() {
        Matching matching = acceptedMatching();
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );

        matching.confirmCancellation(MatchingParticipantRole.FREELANCER);

        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.CANCELED);
        assertThat(matching.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("ACCEPTED 취소 요청은 자동 취소 예정 시각 이후 자동 취소될 수 있다")
    void cancelAutomaticallyIfOverdue_cancelsAcceptedMatchingAfterDeadline() {
        Matching matching = acceptedMatching();
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );

        boolean canceled = matching.cancelAutomaticallyIfOverdue(
                matching.getCancellationAutoCancelAt().plusSeconds(1)
        );

        assertThat(canceled).isTrue();
        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.CANCELED);
        assertThat(matching.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("후기를 작성한 참여자만 완료 확인을 할 수 있고 양측 확인이 끝나면 COMPLETED로 전환된다")
    void confirmCompletion_changesStatusWhenBothParticipantsReviewedAndConfirmed() {
        Matching matching = inProgressMatching();

        matching.submitReview(MatchingParticipantRole.CLIENT, "좋은 협업이었습니다.");
        matching.confirmCompletion(MatchingParticipantRole.CLIENT);

        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.IN_PROGRESS);
        assertThat(matching.isCompletionConfirmedBy(MatchingParticipantRole.CLIENT)).isTrue();
        assertThat(matching.isCompletionConfirmedBy(MatchingParticipantRole.FREELANCER)).isFalse();
        assertThat(matching.getCompleteDate()).isNull();

        matching.submitReview(MatchingParticipantRole.FREELANCER, "명확한 요구사항 덕분에 원활했습니다.");
        matching.confirmCompletion(MatchingParticipantRole.FREELANCER);

        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.COMPLETED);
        assertThat(matching.getCompleteDate()).isNotNull();
    }

    @Test
    @DisplayName("후기를 작성하지 않으면 프로젝트 완료 확인을 할 수 없다")
    void confirmCompletion_throwsWhenReviewMissing() {
        Matching matching = inProgressMatching();

        assertThatThrownBy(() -> matching.confirmCompletion(MatchingParticipantRole.CLIENT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("후기 작성 후 프로젝트 완료를 확인할 수 있습니다.");
    }

    @Test
    @DisplayName("취소 요청 중인 매칭은 후기를 작성할 수 없다")
    void submitReview_throwsWhenCancellationRequested() {
        Matching matching = inProgressMatching();
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_AFTER_PROJECT_SUSPENDED,
                null
        );

        assertThatThrownBy(() -> matching.submitReview(MatchingParticipantRole.CLIENT, "좋은 협업이었습니다."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("취소 요청 중인 매칭은 후기를 작성할 수 없습니다.");
    }

    @Test
    @DisplayName("취소 요청 중인 매칭은 프로젝트 완료 확인을 할 수 없다")
    void confirmCompletion_throwsWhenCancellationRequested() {
        Matching matching = inProgressMatching();
        matching.submitReview(MatchingParticipantRole.CLIENT, "좋은 협업이었습니다.");
        matching.requestCancellation(
                MatchingParticipantRole.FREELANCER,
                MatchingCancellationReason.FREELANCER_AFTER_SCOPE_CHANGED,
                null
        );

        assertThatThrownBy(() -> matching.confirmCompletion(MatchingParticipantRole.CLIENT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("취소 요청 중인 매칭은 완료 확인을 할 수 없습니다.");
    }

    @Test
    @DisplayName("클라이언트 모집 종료로 거절되면 종료 사유와 시각이 함께 기록된다")
    void rejectByClientClosingPosition_recordsReason() {
        Matching matching = proposedMatching();

        matching.rejectByClientClosingPosition("클라이언트가 모집을 종료했습니다.");

        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.REJECTED);
        assertThat(matching.getRejectedAt()).isNotNull();
        assertThat(matching.getCancellationRequestedBy()).isEqualTo(MatchingParticipantRole.CLIENT);
        assertThat(matching.getCancellationReason()).isNull();
        assertThat(matching.getCancellationReasonDetail()).isEqualTo("클라이언트가 모집을 종료했습니다.");
        assertThat(matching.getCancellationRequestedAt()).isEqualTo(matching.getRejectedAt());
    }

    private Matching acceptedMatching() {
        Matching matching = proposedMatching();
        matching.accept();
        return matching;
    }

    private Matching inProgressMatching() {
        Matching matching = acceptedMatching();
        matching.acceptContractStart(MatchingParticipantRole.CLIENT);
        matching.acceptContractStart(MatchingParticipantRole.FREELANCER);
        return matching;
    }

    private Matching proposedMatching() {
        Member client = createMember("client@example.com", "hashed-password", "클라이언트", "010-0000-0001");
        Member freelancer = createMember("freelancer@example.com", "hashed-password", "프리랜서", "010-0000-0002");

        Position position = Position.create("백엔드 개발자");
        Proposal proposal = Proposal.create(client, "AI 매칭 플랫폼", "", "설명", null, null, 8L);
        proposal.startMatching();
        ProposalPosition proposalPosition = proposal.addPosition(
                position,
                "플랫폼 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_000_000L,
                5_000_000L,
                4L,
                null,
                null,
                null
        );

        return Matching.create(null, proposalPosition, client, freelancer);
    }
}
