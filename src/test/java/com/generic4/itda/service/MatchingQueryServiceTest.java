package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingCancellationReason;
import com.generic4.itda.domain.matching.constant.MatchingParticipantRole;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.dto.matching.MatchingDetailViewModel;
import com.generic4.itda.repository.MatchingRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MatchingQueryServiceTest {

    private static final String CLIENT_EMAIL = "client@example.com";
    private static final String FREELANCER_EMAIL = "freelancer@example.com";

    @Mock
    private MatchingRepository matchingRepository;

    @InjectMocks
    private MatchingQueryService matchingQueryService;

    @Test
    @DisplayName("ACCEPTED 상세는 계약 시작 확인 상태와 취소 사유 옵션을 반환한다")
    void getDetail_returnsLifecycleForAcceptedMatching() {
        Matching matching = createAcceptedMatching();
        matching.acceptContractStart(MatchingParticipantRole.CLIENT);
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        MatchingDetailViewModel result = matchingQueryService.getDetail(401L, CLIENT_EMAIL);

        assertThat(result.status()).isEqualTo(MatchingStatus.ACCEPTED);
        assertThat(result.contactVisible()).isTrue();
        assertThat(result.lifecycle().clientContractAccepted()).isTrue();
        assertThat(result.lifecycle().freelancerContractAccepted()).isFalse();
        assertThat(result.lifecycle().currentUserContractAccepted()).isTrue();
        assertThat(result.lifecycle().counterpartContractAccepted()).isFalse();
        assertThat(result.lifecycle().canAcceptContractStart()).isFalse();
        assertThat(result.lifecycle().cancellation().canRequest()).isTrue();
        assertThat(result.lifecycle().cancellation().reasonOptions())
                .extracting("value")
                .containsExactly(
                        "CLIENT_BEFORE_REQUIREMENT_CHANGED",
                        "CLIENT_BEFORE_BUDGET_OR_SCHEDULE_CHANGED",
                        "CLIENT_BEFORE_SELECTED_ANOTHER_FREELANCER",
                        "CLIENT_BEFORE_OTHER"
                );

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("취소 요청이 있으면 수신자에게 확인 가능 상태와 자동 취소 예정 시각을 반환한다")
    void getDetail_returnsCancellationRequestForReceiver() {
        Matching matching = createAcceptedMatching();
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        MatchingDetailViewModel result = matchingQueryService.getDetail(401L, FREELANCER_EMAIL);

        assertThat(result.lifecycle().cancellation().requested()).isTrue();
        assertThat(result.lifecycle().cancellation().requestedByCurrentUser()).isFalse();
        assertThat(result.lifecycle().cancellation().canConfirm()).isTrue();
        assertThat(result.lifecycle().cancellation().canWithdraw()).isFalse();
        assertThat(result.lifecycle().cancellation().requesterRoleLabel()).isEqualTo("클라이언트");
        assertThat(result.lifecycle().cancellation().receiverRoleLabel()).isEqualTo("프리랜서");
        assertThat(result.lifecycle().cancellation().reasonLabel()).isEqualTo("프로젝트 요구사항이 변경되었어요");
        assertThat(result.lifecycle().cancellation().autoCancelAt())
                .isEqualTo(result.lifecycle().cancellation().requestedAt().plusHours(24));
        assertThat(result.lifecycle().canAcceptContractStart()).isFalse();
        assertThat(result.lifecycle().cancellation().reasonOptions()).isEmpty();

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("COMPLETED 상세는 완료 후 상대방 후기를 공개한다")
    void getDetail_returnsCounterpartReviewWhenCompleted() {
        Matching matching = createCompletedMatching();
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        MatchingDetailViewModel result = matchingQueryService.getDetail(401L, CLIENT_EMAIL);

        assertThat(result.status()).isEqualTo(MatchingStatus.COMPLETED);
        assertThat(result.lifecycle().counterpartReviewVisible()).isTrue();
        assertThat(result.lifecycle().currentUserReview()).isEqualTo("좋은 협업이었습니다.");
        assertThat(result.lifecycle().counterpartReview()).isEqualTo("명확한 요구사항 덕분에 원활했습니다.");
        assertThat(result.lifecycle().canSubmitReview()).isFalse();
        assertThat(result.lifecycle().canConfirmCompletion()).isFalse();

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("IN_PROGRESS 상세는 완료 전 작성한 후기를 다시 수정할 수 있다")
    void getDetail_allowsReviewUpdateBeforeCompleted() {
        Matching matching = createInProgressMatching();
        matching.submitReview(MatchingParticipantRole.CLIENT, "수정 전 후기입니다.");
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        MatchingDetailViewModel result = matchingQueryService.getDetail(401L, CLIENT_EMAIL);

        assertThat(result.status()).isEqualTo(MatchingStatus.IN_PROGRESS);
        assertThat(result.lifecycle().currentUserReviewed()).isTrue();
        assertThat(result.lifecycle().currentUserReview()).isEqualTo("수정 전 후기입니다.");
        assertThat(result.lifecycle().canSubmitReview()).isTrue();
        assertThat(result.lifecycle().canConfirmCompletion()).isTrue();

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("계약 진행 후 취소된 상세는 계약 취소 문구를 반환한다")
    void getDetail_returnsContractCancellationLabelsWhenCanceledAfterContractStart() {
        Matching matching = createContractCanceledMatching();
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        MatchingDetailViewModel result = matchingQueryService.getDetail(401L, CLIENT_EMAIL);

        assertThat(result.status()).isEqualTo(MatchingStatus.CANCELED);
        assertThat(result.header().statusLabel()).isEqualTo("계약 취소");
        assertThat(result.summary().headline()).isEqualTo("계약이 취소되었습니다.");
        assertThat(result.summary().helperMessage()).isEqualTo("취소된 계약입니다. 필요한 경우 진행 이력과 연락처 정보를 확인해보세요.");
        assertThat(result.lifecycle().contractCancellation()).isTrue();
        assertThat(result.lifecycle().cancellation().requested()).isTrue();
        assertThat(result.lifecycle().cancellation().canConfirm()).isFalse();
        assertThat(result.lifecycle().cancellation().canWithdraw()).isFalse();
        assertThat(result.lifecycle().cancellation().requesterRoleLabel()).isEqualTo("클라이언트");
        assertThat(result.lifecycle().cancellation().receiverRoleLabel()).isEqualTo("프리랜서");
        assertThat(result.lifecycle().cancellation().reasonLabel()).isEqualTo("프로젝트가 중단되었어요");
        assertThat(result.lifecycle().cancellation().requestedAt()).isNotNull();
        assertThat(result.timeline().get(result.timeline().size() - 1).actionLabel()).isEqualTo("계약 취소");
        assertThat(result.timeline().get(result.timeline().size() - 1).description()).isEqualTo("계약이 취소되었습니다.");

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("계약 전 취소된 상세도 취소 요청 사유 정보를 반환한다")
    void getDetail_returnsCancellationReasonWhenCanceledBeforeContractStart() {
        Matching matching = createMatchingCanceledMatching();
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        MatchingDetailViewModel result = matchingQueryService.getDetail(401L, FREELANCER_EMAIL);

        assertThat(result.status()).isEqualTo(MatchingStatus.CANCELED);
        assertThat(result.header().statusLabel()).isEqualTo("매칭 취소");
        assertThat(result.lifecycle().contractCancellation()).isFalse();
        assertThat(result.lifecycle().cancellation().requested()).isTrue();
        assertThat(result.lifecycle().cancellation().requestedByCurrentUser()).isFalse();
        assertThat(result.lifecycle().cancellation().canConfirm()).isFalse();
        assertThat(result.lifecycle().cancellation().canWithdraw()).isFalse();
        assertThat(result.lifecycle().cancellation().requesterRoleLabel()).isEqualTo("클라이언트");
        assertThat(result.lifecycle().cancellation().receiverRoleLabel()).isEqualTo("프리랜서");
        assertThat(result.lifecycle().cancellation().reasonLabel()).isEqualTo("프로젝트 요구사항이 변경되었어요");
        assertThat(result.lifecycle().cancellation().requestedAt()).isNotNull();

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("매칭 당사자가 아니면 상세를 조회할 수 없다")
    void getDetail_throwsWhenViewerIsNotParticipant() {
        Matching matching = createAcceptedMatching();
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        assertThatThrownBy(() -> matchingQueryService.getDetail(401L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 매칭 정보에 접근할 수 없습니다.");

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    private Matching createAcceptedMatching() {
        Matching matching = createProposedMatching();
        matching.accept();
        return matching;
    }

    private Matching createCompletedMatching() {
        Matching matching = createInProgressMatching();
        matching.submitReview(MatchingParticipantRole.CLIENT, "좋은 협업이었습니다.");
        matching.submitReview(MatchingParticipantRole.FREELANCER, "명확한 요구사항 덕분에 원활했습니다.");
        matching.confirmCompletion(MatchingParticipantRole.CLIENT);
        matching.confirmCompletion(MatchingParticipantRole.FREELANCER);
        return matching;
    }

    private Matching createInProgressMatching() {
        Matching matching = createAcceptedMatching();
        matching.acceptContractStart(MatchingParticipantRole.CLIENT);
        matching.acceptContractStart(MatchingParticipantRole.FREELANCER);
        return matching;
    }

    private Matching createContractCanceledMatching() {
        Matching matching = createInProgressMatching();
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_AFTER_PROJECT_SUSPENDED,
                null
        );
        matching.confirmCancellation(MatchingParticipantRole.FREELANCER);
        return matching;
    }

    private Matching createMatchingCanceledMatching() {
        Matching matching = createAcceptedMatching();
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );
        matching.confirmCancellation(MatchingParticipantRole.FREELANCER);
        return matching;
    }

    private Matching createProposedMatching() {
        Member client = createMember(CLIENT_EMAIL, "hashed-password", "클라이언트", "010-0000-0001");
        Member freelancer = createMember(FREELANCER_EMAIL, "hashed-password", "프리랜서", "010-0000-0002");

        Position position = Position.create("백엔드 개발자");
        ReflectionTestUtils.setField(position, "id", 101L);

        Proposal proposal = Proposal.create(
                client,
                "AI 매칭 플랫폼",
                "원본 입력",
                "설명",
                null,
                null,
                8L
        );
        proposal.startMatching();
        ReflectionTestUtils.setField(proposal, "id", 200L);

        ProposalPosition proposalPosition = proposal.addPosition(
                position,
                "플랫폼 백엔드 개발자",
                ProposalWorkType.REMOTE,
                2L,
                3_000_000L,
                5_000_000L,
                4L,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(proposalPosition, "id", 201L);

        Resume resume = Resume.create(
                freelancer,
                "자기소개입니다.",
                (byte) 3,
                new CareerPayload(),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                null
        );
        ReflectionTestUtils.setField(resume, "id", 301L);

        Matching matching = Matching.create(resume, proposalPosition, client, freelancer);
        ReflectionTestUtils.setField(matching, "id", 401L);
        return matching;
    }
}
