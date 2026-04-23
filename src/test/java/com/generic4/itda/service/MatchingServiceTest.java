package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingCancellationReason;
import com.generic4.itda.domain.matching.constant.MatchingParticipantRole;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.ProposalPositionRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    private static final Long RESULT_ID = 101L;
    private static final String CLIENT_EMAIL = "client@example.com";

    @InjectMocks
    private MatchingService matchingService;

    @Mock
    private RecommendationResultRepository recommendationResultRepository;

    @Mock
    private MatchingRepository matchingRepository;

    @Mock
    private ProposalPositionRepository proposalPositionRepository;

    @Test
    @DisplayName("클라이언트는 추천 결과를 기준으로 PROPOSED 매칭 요청을 생성할 수 있다")
    void request_createsProposedMatchingWhenValid() {
        RecommendationResult recommendationResult = createRecommendationResult(
                ProposalStatus.MATCHING,
                ProposalPositionStatus.OPEN,
                CLIENT_EMAIL
        );

        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.of(recommendationResult));
        given(matchingRepository.existsByProposalPosition_IdAndResume_IdAndStatusIn(
                eq(201L),
                eq(301L),
                eq(EnumSet.of(MatchingStatus.PROPOSED, MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS))
        )).willReturn(false);
        given(matchingRepository.save(any(Matching.class))).willAnswer(invocation -> {
            Matching matching = invocation.getArgument(0);
            ReflectionTestUtils.setField(matching, "id", 401L);
            return matching;
        });

        Matching saved = matchingService.request(RESULT_ID, CLIENT_EMAIL);

        assertThat(saved.getId()).isEqualTo(401L);
        assertThat(saved.getStatus()).isEqualTo(MatchingStatus.PROPOSED);
        assertThat(saved.getRequestedAt()).isNotNull();
        assertThat(saved.getAcceptedAt()).isNull();
        assertThat(saved.getRejectedAt()).isNull();
        assertThat(saved.getProposalPosition().getId()).isEqualTo(201L);
        assertThat(saved.getResume().getId()).isEqualTo(301L);
        assertThat(saved.getClientMember().getEmail().getValue()).isEqualTo(CLIENT_EMAIL);
        assertThat(saved.getFreelancerMember().getEmail().getValue()).isEqualTo("freelancer@example.com");

        ArgumentCaptor<Matching> captor = ArgumentCaptor.forClass(Matching.class);
        verify(matchingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MatchingStatus.PROPOSED);
        assertThat(captor.getValue().getRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("추천 결과가 없으면 매칭 요청을 생성할 수 없다")
    void request_throwsWhenRecommendationResultNotFound() {
        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.request(RESULT_ID, CLIENT_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("추천 결과를 찾을 수 없습니다. id=" + RESULT_ID);

        verify(matchingRepository, never()).save(any(Matching.class));
    }

    @Test
    @DisplayName("본인 제안서가 아니면 매칭 요청을 생성할 수 없다")
    void request_throwsWhenClientDoesNotOwnProposal() {
        RecommendationResult recommendationResult = createRecommendationResult(
                ProposalStatus.MATCHING,
                ProposalPositionStatus.OPEN,
                "owner@example.com"
        );
        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.of(recommendationResult));

        assertThatThrownBy(() -> matchingService.request(RESULT_ID, CLIENT_EMAIL))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인 제안서에 대해서만 매칭 요청을 보낼 수 있습니다.");

        verify(matchingRepository, never()).save(any(Matching.class));
    }

    @Test
    @DisplayName("MATCHING 상태가 아닌 제안서에는 매칭 요청을 생성할 수 없다")
    void request_throwsWhenProposalStatusIsNotMatching() {
        RecommendationResult recommendationResult = createRecommendationResult(
                ProposalStatus.WRITING,
                ProposalPositionStatus.OPEN,
                CLIENT_EMAIL
        );
        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.of(recommendationResult));

        assertThatThrownBy(() -> matchingService.request(RESULT_ID, CLIENT_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MATCHING 상태의 제안서에 대해서만 매칭 요청을 보낼 수 있습니다.");

        verify(matchingRepository, never()).save(any(Matching.class));
    }

    @Test
    @DisplayName("OPEN 상태가 아닌 모집 포지션에는 매칭 요청을 생성할 수 없다")
    void request_throwsWhenPositionStatusIsNotOpen() {
        RecommendationResult recommendationResult = createRecommendationResult(
                ProposalStatus.MATCHING,
                ProposalPositionStatus.FULL,
                CLIENT_EMAIL
        );
        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.of(recommendationResult));

        assertThatThrownBy(() -> matchingService.request(RESULT_ID, CLIENT_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPEN 상태의 모집 포지션에 대해서만 매칭 요청을 보낼 수 있습니다.");

        verify(matchingRepository, never()).save(any(Matching.class));
    }

    @Test
    @DisplayName("같은 포지션과 이력서에 활성 매칭이 있으면 중복 요청할 수 없다")
    void request_throwsWhenActiveMatchingAlreadyExists() {
        RecommendationResult recommendationResult = createRecommendationResult(
                ProposalStatus.MATCHING,
                ProposalPositionStatus.OPEN,
                CLIENT_EMAIL
        );
        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.of(recommendationResult));
        given(matchingRepository.existsByProposalPosition_IdAndResume_IdAndStatusIn(
                eq(201L),
                eq(301L),
                eq(EnumSet.of(MatchingStatus.PROPOSED, MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS))
        )).willReturn(true);

        assertThatThrownBy(() -> matchingService.request(RESULT_ID, CLIENT_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 요청했거나 진행 중인 매칭입니다.");

        verify(matchingRepository, never()).save(any(Matching.class));
    }

    @Test
    @DisplayName("프리랜서는 본인에게 온 PROPOSED 매칭을 수락할 수 있다")
    void accept_marksMatchingAcceptedAndPositionFullWhenCapacityReached() {
        Matching matching = createMatching(
                MatchingStatus.PROPOSED,
                ProposalPositionStatus.OPEN,
                1L
        );

        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));
        given(proposalPositionRepository.findByIdForUpdate(201L))
                .willReturn(Optional.of(matching.getProposalPosition()));
        given(matchingRepository.countByProposalPosition_IdAndStatusIn(
                eq(201L),
                eq(EnumSet.of(MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS))
        )).willReturn(0L, 1L);

        Matching accepted = matchingService.accept(401L, "freelancer@example.com");

        assertThat(accepted.getStatus()).isEqualTo(MatchingStatus.ACCEPTED);
        assertThat(accepted.getAcceptedAt()).isNotNull();
        assertThat(accepted.getRejectedAt()).isNull();
        assertThat(accepted.getProposalPosition().getStatus()).isEqualTo(ProposalPositionStatus.FULL);
        verify(proposalPositionRepository).findByIdForUpdate(201L);
    }

    @Test
    @DisplayName("정원이 남아 있으면 수락 후에도 모집 포지션은 OPEN 상태를 유지한다")
    void accept_keepsPositionOpenWhenCapacityRemains() {
        Matching matching = createMatching(
                MatchingStatus.PROPOSED,
                ProposalPositionStatus.OPEN,
                2L
        );

        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));
        given(proposalPositionRepository.findByIdForUpdate(201L))
                .willReturn(Optional.of(matching.getProposalPosition()));
        given(matchingRepository.countByProposalPosition_IdAndStatusIn(
                eq(201L),
                eq(EnumSet.of(MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS))
        )).willReturn(0L, 1L);

        Matching accepted = matchingService.accept(401L, "freelancer@example.com");

        assertThat(accepted.getStatus()).isEqualTo(MatchingStatus.ACCEPTED);
        assertThat(accepted.getAcceptedAt()).isNotNull();
        assertThat(accepted.getRejectedAt()).isNull();
        assertThat(accepted.getProposalPosition().getStatus()).isEqualTo(ProposalPositionStatus.OPEN);
    }

    @Test
    @DisplayName("이미 정원이 찬 포지션의 매칭은 수락할 수 없다")
    void accept_throwsWhenCapacityAlreadyFull() {
        Matching matching = createMatching(
                MatchingStatus.PROPOSED,
                ProposalPositionStatus.OPEN,
                1L
        );

        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));
        given(proposalPositionRepository.findByIdForUpdate(201L))
                .willReturn(Optional.of(matching.getProposalPosition()));
        given(matchingRepository.countByProposalPosition_IdAndStatusIn(
                eq(201L),
                eq(EnumSet.of(MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS))
        )).willReturn(1L);

        assertThatThrownBy(() -> matchingService.accept(401L, "freelancer@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("정원이 이미 찬 모집 포지션입니다.");

        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.PROPOSED);
        assertThat(matching.getAcceptedAt()).isNull();
        assertThat(matching.getRejectedAt()).isNull();
    }

    @Test
    @DisplayName("FULL 상태 포지션의 매칭은 수락할 수 없다")
    void accept_throwsWhenPositionStatusIsFull() {
        Matching matching = createMatching(
                MatchingStatus.PROPOSED,
                ProposalPositionStatus.FULL,
                1L
        );

        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));
        given(proposalPositionRepository.findByIdForUpdate(201L))
                .willReturn(Optional.of(matching.getProposalPosition()));

        assertThatThrownBy(() -> matchingService.accept(401L, "freelancer@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("정원이 이미 찬 모집 포지션입니다.");

        assertThat(matching.getAcceptedAt()).isNull();
        assertThat(matching.getRejectedAt()).isNull();
    }

    @Test
    @DisplayName("다른 프리랜서는 매칭 요청을 수락할 수 없다")
    void accept_throwsWhenFreelancerDoesNotOwnMatching() {
        Matching matching = createMatching(
                MatchingStatus.PROPOSED,
                ProposalPositionStatus.OPEN,
                1L
        );

        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        assertThatThrownBy(() -> matchingService.accept(401L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인에게 온 매칭 요청에만 응답할 수 있습니다.");

        assertThat(matching.getAcceptedAt()).isNull();
        assertThat(matching.getRejectedAt()).isNull();
    }

    @Test
    @DisplayName("프리랜서는 본인에게 온 PROPOSED 매칭을 거절할 수 있다")
    void reject_marksMatchingRejected() {
        Matching matching = createMatching(
                MatchingStatus.PROPOSED,
                ProposalPositionStatus.OPEN,
                1L
        );

        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        Matching rejected = matchingService.reject(401L, "freelancer@example.com");

        assertThat(rejected.getStatus()).isEqualTo(MatchingStatus.REJECTED);
        assertThat(rejected.getRejectedAt()).isNotNull();
        assertThat(rejected.getAcceptedAt()).isNull();
        assertThat(rejected.getProposalPosition().getStatus()).isEqualTo(ProposalPositionStatus.OPEN);
    }

    @Test
    @DisplayName("다른 프리랜서는 매칭 요청을 거절할 수 없다")
    void reject_throwsWhenFreelancerDoesNotOwnMatching() {
        Matching matching = createMatching(
                MatchingStatus.PROPOSED,
                ProposalPositionStatus.OPEN,
                1L
        );

        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        assertThatThrownBy(() -> matchingService.reject(401L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인에게 온 매칭 요청에만 응답할 수 있습니다.");

        assertThat(matching.getAcceptedAt()).isNull();
        assertThat(matching.getRejectedAt()).isNull();
    }

    @Test
    @DisplayName("양측이 계약 시작을 수락하면 IN_PROGRESS 상태가 된다")
    void acceptContractStart_changesStatusWhenBothParticipantsAccepted() {
        Matching matching = createMatching(
                MatchingStatus.ACCEPTED,
                ProposalPositionStatus.FULL,
                1L
        );
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        Matching waiting = matchingService.acceptContractStart(401L, CLIENT_EMAIL);

        assertThat(waiting.getStatus()).isEqualTo(MatchingStatus.ACCEPTED);
        assertThat(waiting.isContractStartAcceptedBy(MatchingParticipantRole.CLIENT)).isTrue();
        assertThat(waiting.isContractStartAcceptedBy(MatchingParticipantRole.FREELANCER)).isFalse();

        Matching inProgress = matchingService.acceptContractStart(401L, "freelancer@example.com");

        assertThat(inProgress.getStatus()).isEqualTo(MatchingStatus.IN_PROGRESS);
        assertThat(inProgress.isContractStartAcceptedBy(MatchingParticipantRole.FREELANCER)).isTrue();
        assertThat(inProgress.getContractDate()).isNotNull();
    }

    @Test
    @DisplayName("매칭 당사자는 취소 요청을 보낼 수 있다")
    void requestCancellation_createsCancellationRequest() {
        Matching matching = createMatching(
                MatchingStatus.ACCEPTED,
                ProposalPositionStatus.FULL,
                1L
        );
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        Matching requested = matchingService.requestCancellation(
                401L,
                CLIENT_EMAIL,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );

        assertThat(requested.hasCancellationRequest()).isTrue();
        assertThat(requested.getCancellationRequestedBy()).isEqualTo(MatchingParticipantRole.CLIENT);
        assertThat(requested.getCancellationReason())
                .isEqualTo(MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED);
        assertThat(requested.getCancellationAutoCancelAt())
                .isEqualTo(requested.getCancellationRequestedAt().plusHours(24));
    }

    @Test
    @DisplayName("취소 요청자는 취소 요청을 철회할 수 있다")
    void withdrawCancellation_clearsCancellationRequest() {
        Matching matching = createMatching(
                MatchingStatus.ACCEPTED,
                ProposalPositionStatus.FULL,
                1L
        );
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        Matching withdrawn = matchingService.withdrawCancellation(401L, CLIENT_EMAIL);

        assertThat(withdrawn.hasCancellationRequest()).isFalse();
        assertThat(withdrawn.getCancellationRequestedBy()).isNull();
        assertThat(withdrawn.getCancellationReason()).isNull();
    }

    @Test
    @DisplayName("취소 요청 수신자가 확인하면 CANCELED 상태가 되고 포지션 정원을 재계산한다")
    void confirmCancellation_changesStatusAndRecalculatesPositionStatus() {
        Matching matching = createMatching(
                MatchingStatus.ACCEPTED,
                ProposalPositionStatus.FULL,
                1L
        );
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));
        given(matchingRepository.countByProposalPosition_IdAndStatusIn(
                eq(201L),
                eq(EnumSet.of(MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS))
        )).willReturn(0L);

        Matching canceled = matchingService.confirmCancellation(401L, "freelancer@example.com");

        assertThat(canceled.getStatus()).isEqualTo(MatchingStatus.CANCELED);
        assertThat(canceled.getCanceledAt()).isNotNull();
        assertThat(canceled.getProposalPosition().getStatus()).isEqualTo(ProposalPositionStatus.OPEN);
    }

    @Test
    @DisplayName("자동 취소 예정 시각이 지난 ACCEPTED 취소 요청을 CANCELED로 전환한다")
    void cancelOverdueAcceptedCancellationRequests_cancelsDueRequests() {
        Matching matching = createMatching(
                MatchingStatus.ACCEPTED,
                ProposalPositionStatus.FULL,
                1L
        );
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );
        LocalDateTime now = matching.getCancellationAutoCancelAt().plusSeconds(1);
        given(matchingRepository.findAcceptedCancellationRequestsDue(now)).willReturn(List.of(matching));
        given(matchingRepository.countByProposalPosition_IdAndStatusIn(
                eq(201L),
                eq(EnumSet.of(MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS))
        )).willReturn(0L);

        int canceledCount = matchingService.cancelOverdueAcceptedCancellationRequests(now);

        assertThat(canceledCount).isEqualTo(1);
        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.CANCELED);
        assertThat(matching.getProposalPosition().getStatus()).isEqualTo(ProposalPositionStatus.OPEN);
    }

    @Test
    @DisplayName("진행 중인 매칭에서 양측 후기를 작성하고 완료 확인하면 COMPLETED 상태가 된다")
    void submitReviewAndConfirmCompletion_changesStatusWhenBothParticipantsConfirmed() {
        Matching matching = createMatching(
                MatchingStatus.IN_PROGRESS,
                ProposalPositionStatus.FULL,
                1L
        );
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));
        given(matchingRepository.countByProposalPosition_IdAndStatusIn(
                eq(201L),
                eq(EnumSet.of(MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS))
        )).willReturn(0L);

        matchingService.submitReview(401L, CLIENT_EMAIL, "좋은 협업이었습니다.");
        matchingService.confirmCompletion(401L, CLIENT_EMAIL);

        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.IN_PROGRESS);
        assertThat(matching.hasReviewBy(MatchingParticipantRole.CLIENT)).isTrue();
        assertThat(matching.isCompletionConfirmedBy(MatchingParticipantRole.CLIENT)).isTrue();

        matchingService.submitReview(401L, "freelancer@example.com", "명확한 요구사항 덕분에 원활했습니다.");
        Matching completed = matchingService.confirmCompletion(401L, "freelancer@example.com");

        assertThat(completed.getStatus()).isEqualTo(MatchingStatus.COMPLETED);
        assertThat(completed.getCompleteDate()).isNotNull();
        assertThat(completed.getProposalPosition().getStatus()).isEqualTo(ProposalPositionStatus.OPEN);
    }

    private RecommendationResult createRecommendationResult(
            ProposalStatus proposalStatus,
            ProposalPositionStatus positionStatus,
            String proposalOwnerEmail
    ) {
        Member clientMember = createMember(proposalOwnerEmail, "hashed-password", "클라이언트", "010-0000-0001");
        Member freelancerMember = createMember("freelancer@example.com", "hashed-password", "프리랜서", "010-0000-0002");

        Position position = Position.create("백엔드 개발자");
        ReflectionTestUtils.setField(position, "id", 101L);

        Proposal proposal = Proposal.create(
                clientMember,
                "AI 매칭 플랫폼",
                "원본 입력",
                "설명",
                null,
                null,
                8L
        );
        if (proposalStatus == ProposalStatus.MATCHING) {
            proposal.startMatching();
        }
        if (proposalStatus == ProposalStatus.COMPLETE) {
            proposal.startMatching();
            proposal.complete();
        }

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
        if (positionStatus != ProposalPositionStatus.OPEN) {
            proposalPosition.changeStatus(positionStatus);
        }

        ReflectionTestUtils.setField(proposal, "id", 200L);
        ReflectionTestUtils.setField(proposalPosition, "id", 201L);

        Resume resume = Resume.create(
                freelancerMember,
                "자기소개입니다.",
                (byte) 3,
                new CareerPayload(),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                null
        );
        ReflectionTestUtils.setField(resume, "id", 301L);

        RecommendationRun run = RecommendationRun.create(
                proposalPosition,
                "fp-001",
                RecommendationAlgorithm.HEURISTIC_V1,
                3
        );
        ReflectionTestUtils.setField(run, "id", 501L);

        RecommendationResult recommendationResult = RecommendationResult.create(
                run,
                resume,
                1,
                new BigDecimal("0.9100"),
                new BigDecimal("0.8800"),
                new ReasonFacts(List.of("Java"), List.of("플랫폼"), 3, List.of("Spring Boot"))
        );
        ReflectionTestUtils.setField(recommendationResult, "id", RESULT_ID);
        return recommendationResult;
    }

    private Matching createMatching(
            MatchingStatus matchingStatus,
            ProposalPositionStatus positionStatus,
            Long headCount
    ) {
        RecommendationResult recommendationResult = createRecommendationResult(
                ProposalStatus.MATCHING,
                positionStatus,
                CLIENT_EMAIL
        );

        ProposalPosition proposalPosition = recommendationResult.getRecommendationRun().getProposalPosition();
        ReflectionTestUtils.setField(proposalPosition, "headCount", headCount);

        Matching matching = Matching.create(
                recommendationResult.getResume(),
                proposalPosition,
                proposalPosition.getProposal().getMember(),
                recommendationResult.getResume().getMember()
        );
        ReflectionTestUtils.setField(matching, "id", 401L);
        ReflectionTestUtils.setField(matching, "status", matchingStatus);
        return matching;
    }
}
