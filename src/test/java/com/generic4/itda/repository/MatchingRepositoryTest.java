package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.RepositoryTest;
import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingCancellationReason;
import com.generic4.itda.domain.matching.constant.MatchingParticipantRole;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@RepositoryTest
class MatchingRepositoryTest {

    @Autowired
    private MatchingRepository matchingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private EntityManager em;

    private Proposal proposal;
    private ProposalPosition backendPosition;
    private Resume freelancerResume;
    private Member clientMember;
    private Member freelancerMember;

    @BeforeEach
    void setUp() {
        clientMember = memberRepository.save(
                createMember("client@test.com", "pw", "클라이언트", "010-0000-0001"));
        freelancerMember = memberRepository.save(
                createMember("freelancer@test.com", "pw", "프리랜서", "010-0000-0002"));

        Position backend = persistPosition("백엔드 개발자");

        proposal = Proposal.create(clientMember, "AI 매칭 플랫폼", "원문", null, null, null, null);
        backendPosition = proposal.addPosition(
                backend,
                "플랫폼 백엔드 개발자",
                null,
                2L,
                3_000_000L,
                5_000_000L,
                null,
                null,
                null,
                null
        );
        proposalRepository.saveAndFlush(proposal);

        freelancerResume = resumeRepository.saveAndFlush(
                Resume.create(
                        freelancerMember,
                        "자기소개입니다.",
                        (byte) 3,
                        new CareerPayload(),
                        WorkType.REMOTE,
                        ResumeWritingStatus.DONE,
                        null
                )
        );
    }

    @Nested
    @DisplayName("existsByProposalPosition_IdAndResume_IdAndStatusIn")
    class ExistsActiveMatching {

        @Test
        @DisplayName("같은 포지션과 이력서 조합의 활성 매칭이 있으면 true를 반환한다")
        void returnsTrueWhenActiveMatchingExists() {
            Matching matching = saveMatching(backendPosition, freelancerResume, MatchingStatus.PROPOSED);

            boolean result = matchingRepository.existsByProposalPosition_IdAndResume_IdAndStatusIn(
                    backendPosition.getId(),
                    freelancerResume.getId(),
                    EnumSet.of(MatchingStatus.PROPOSED, MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS)
            );

            assertThat(result).isTrue();
            assertThat(matching.getId()).isNotNull();
        }

        @Test
        @DisplayName("같은 조합이어도 종료 상태만 있으면 false를 반환한다")
        void returnsFalseWhenOnlyTerminalMatchingExists() {
            saveMatching(backendPosition, freelancerResume, MatchingStatus.REJECTED);

            boolean result = matchingRepository.existsByProposalPosition_IdAndResume_IdAndStatusIn(
                    backendPosition.getId(),
                    freelancerResume.getId(),
                    EnumSet.of(MatchingStatus.PROPOSED, MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS)
            );

            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("정원 점유 상태만 countByProposalPosition_IdAndStatusIn으로 집계할 수 있다")
    void countByProposalPositionIdAndStatusIn_countsOnlyRequestedStatuses() {
        saveMatching(backendPosition, freelancerResume, MatchingStatus.ACCEPTED);

        Member secondFreelancer = memberRepository.save(
                createMember("freelancer2@test.com", "pw", "프리랜서2", "010-0000-0003"));
        Resume secondResume = resumeRepository.saveAndFlush(
                Resume.create(
                        secondFreelancer,
                        "또 다른 자기소개입니다.",
                        (byte) 5,
                        new CareerPayload(),
                        WorkType.REMOTE,
                        ResumeWritingStatus.DONE,
                        null
                )
        );
        saveMatching(backendPosition, secondResume, MatchingStatus.IN_PROGRESS);

        Member thirdFreelancer = memberRepository.save(
                createMember("freelancer3@test.com", "pw", "프리랜서3", "010-0000-0004"));
        Resume thirdResume = resumeRepository.saveAndFlush(
                Resume.create(
                        thirdFreelancer,
                        "세 번째 자기소개입니다.",
                        (byte) 1,
                        new CareerPayload(),
                        WorkType.SITE,
                        ResumeWritingStatus.DONE,
                        null
                )
        );
        saveMatching(backendPosition, thirdResume, MatchingStatus.PROPOSED);

        long occupiedCount = matchingRepository.countByProposalPosition_IdAndStatusIn(
                backendPosition.getId(),
                EnumSet.of(MatchingStatus.ACCEPTED, MatchingStatus.IN_PROGRESS)
        );

        assertThat(occupiedCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("findDetailById는 매칭 생성/응답에 필요한 연관 그래프를 함께 조회한다")
    void findDetailById_fetchesMatchingGraph() {
        Matching matching = saveMatching(backendPosition, freelancerResume, MatchingStatus.PROPOSED);
        em.clear();

        Matching found = matchingRepository.findDetailById(matching.getId()).orElseThrow();

        PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(util.isLoaded(found, "proposalPosition")).isTrue();
        assertThat(util.isLoaded(found, "resume")).isTrue();
        assertThat(util.isLoaded(found, "clientMember")).isTrue();
        assertThat(util.isLoaded(found, "freelancerMember")).isTrue();

        ProposalPosition foundPosition = found.getProposalPosition();
        assertThat(util.isLoaded(foundPosition, "proposal")).isTrue();
        assertThat(foundPosition.getProposal().getId()).isEqualTo(proposal.getId());

        assertThat(util.isLoaded(found.getResume(), "member")).isTrue();
        assertThat(found.getResume().getMember().getId()).isEqualTo(freelancerMember.getId());
    }

    @Test
    @DisplayName("findAcceptedCancellationRequestsDue는 기한이 지난 ACCEPTED 취소 요청만 반환한다")
    void findAcceptedCancellationRequestsDue_returnsOnlyDueAcceptedCancellationRequests() {
        Matching dueAccepted = saveMatching(backendPosition, freelancerResume, MatchingStatus.ACCEPTED);
        dueAccepted.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );
        LocalDateTime now = LocalDateTime.of(2026, 4, 23, 12, 0);
        ReflectionTestUtils.setField(dueAccepted, "cancellationAutoCancelAt", now.minusMinutes(1));

        Member secondFreelancer = memberRepository.save(
                createMember("freelancer2@test.com", "pw", "프리랜서2", "010-0000-0003"));
        Resume secondResume = resumeRepository.saveAndFlush(
                Resume.create(
                        secondFreelancer,
                        "두 번째 자기소개입니다.",
                        (byte) 4,
                        new CareerPayload(),
                        WorkType.REMOTE,
                        ResumeWritingStatus.DONE,
                        null
                )
        );
        Matching waitingAccepted = saveMatching(backendPosition, secondResume, MatchingStatus.ACCEPTED);
        waitingAccepted.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_BEFORE_REQUIREMENT_CHANGED,
                null
        );
        ReflectionTestUtils.setField(waitingAccepted, "cancellationAutoCancelAt", now.plusMinutes(1));

        Member thirdFreelancer = memberRepository.save(
                createMember("freelancer3@test.com", "pw", "프리랜서3", "010-0000-0004"));
        Resume thirdResume = resumeRepository.saveAndFlush(
                Resume.create(
                        thirdFreelancer,
                        "세 번째 자기소개입니다.",
                        (byte) 5,
                        new CareerPayload(),
                        WorkType.REMOTE,
                        ResumeWritingStatus.DONE,
                        null
                )
        );
        Matching inProgress = saveMatching(backendPosition, thirdResume, MatchingStatus.IN_PROGRESS);
        inProgress.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_AFTER_PROJECT_SUSPENDED,
                null
        );

        em.flush();
        em.clear();

        List<Matching> result = matchingRepository.findAcceptedCancellationRequestsDue(now);

        assertThat(result).extracting(Matching::getId)
                .contains(dueAccepted.getId())
                .doesNotContain(waitingAccepted.getId())
                .doesNotContain(inProgress.getId());
    }

    @Nested
    @DisplayName("findByProposalPositionIdAndResumeIdIn")
    class FindByProposalPositionIdAndResumeIdIn {

        @Test
        @DisplayName("포지션 ID와 resume ID 목록에 일치하는 매칭을 반환한다")
        void returnsMatchingForGivenPositionAndResumeIds() {
            Matching matching = saveMatching(backendPosition, freelancerResume, MatchingStatus.PROPOSED);
            em.clear();

            List<Matching> result = matchingRepository.findByProposalPositionIdAndResumeIdIn(
                    backendPosition.getId(),
                    List.of(freelancerResume.getId())
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(matching.getId());
            assertThat(result.get(0).getStatus()).isEqualTo(MatchingStatus.PROPOSED);
        }

        @Test
        @DisplayName("resume ID 목록에 없는 resume의 매칭은 반환하지 않는다")
        void excludesMatchingsForUnlistedResumeIds() {
            saveMatching(backendPosition, freelancerResume, MatchingStatus.PROPOSED);
            em.clear();

            List<Matching> result = matchingRepository.findByProposalPositionIdAndResumeIdIn(
                    backendPosition.getId(),
                    List.of(-999L)
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 포지션의 매칭은 반환하지 않는다")
        void excludesMatchingsForDifferentPosition() {
            // 다른 제안서와 포지션 생성
            Member otherClient = memberRepository.save(
                    createMember("other-client@test.com", "pw", "다른클라이언트", "010-0000-0099"));
            Position frontend = persistPosition("프론트엔드 개발자");
            Proposal otherProposal = Proposal.create(otherClient, "다른 제안서", "원문", null, null, null, null);
            ProposalPosition frontendPosition = otherProposal.addPosition(
                    frontend, "프론트엔드 개발자", null, 1L, 1_000_000L, 2_000_000L, null, null, null, null);
            proposalRepository.saveAndFlush(otherProposal);

            saveMatching(frontendPosition, freelancerResume, MatchingStatus.PROPOSED);
            em.clear();

            // backendPosition으로 조회하면 frontendPosition 매칭은 안 나와야 함
            List<Matching> result = matchingRepository.findByProposalPositionIdAndResumeIdIn(
                    backendPosition.getId(),
                    List.of(freelancerResume.getId())
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("여러 resume ID에 대한 매칭을 한 번에 반환한다")
        void returnsMultipleMatchingsForBatchResumeIds() {
            Member secondFreelancer = memberRepository.save(
                    createMember("freelancer2@test.com", "pw", "프리랜서2", "010-0000-0010"));
            Resume secondResume = resumeRepository.saveAndFlush(
                    Resume.create(secondFreelancer, "두 번째 소개", (byte) 3,
                            new CareerPayload(), WorkType.REMOTE, ResumeWritingStatus.DONE, null));

            saveMatching(backendPosition, freelancerResume, MatchingStatus.PROPOSED);
            saveMatching(backendPosition, secondResume, MatchingStatus.ACCEPTED);
            em.clear();

            List<Matching> result = matchingRepository.findByProposalPositionIdAndResumeIdIn(
                    backendPosition.getId(),
                    List.of(freelancerResume.getId(), secondResume.getId())
            );

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Matching::getStatus)
                    .containsExactlyInAnyOrder(MatchingStatus.PROPOSED, MatchingStatus.ACCEPTED);
        }
    }

    @Test
    @DisplayName("getDashboardItems는 프리랜서가 받은 매칭 목록을 최신순으로 반환한다")
    void getDashboardItems_returnsFreelancerDashboardItems() {
        Matching matching = saveMatching(backendPosition, freelancerResume, MatchingStatus.PROPOSED);
        em.flush();
        em.clear();

        List<?> items = matchingRepository.getDashboardItems(freelancerMember.getEmail().getValue(), null, null);

        assertThat(items).hasSize(1);
        assertThat(matching.getId()).isNotNull();
        assertThat(items)
                .extracting("matchingId", "proposalId", "proposalPositionId")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        matching.getId(),
                        proposal.getId(),
                        backendPosition.getId()
                ));
    }

    @Test
    @DisplayName("getDashboardItems는 대시보드 상태값 NEW/IN_PROGRESS/COMPLETED를 실제 매칭 상태에 맞게 필터링한다")
    void getDashboardItems_filtersByDashboardStatusBuckets() {
        Matching proposedMatching = saveMatching(backendPosition, freelancerResume, MatchingStatus.PROPOSED);

        ProposalPosition acceptedPosition = saveAdditionalPosition("AI 추천 서비스", "AI 엔지니어");
        Matching acceptedMatching = saveMatching(acceptedPosition, freelancerResume, MatchingStatus.ACCEPTED);

        ProposalPosition completedPosition = saveAdditionalPosition("운영 자동화 툴", "운영 엔지니어");
        Matching completedMatching = saveMatching(completedPosition, freelancerResume, MatchingStatus.COMPLETED);

        em.flush();
        em.clear();

        List<FreelancerDashboardItem> newItems = matchingRepository.getDashboardItems(
                freelancerMember.getEmail().getValue(), "NEW", null);
        List<FreelancerDashboardItem> inProgressItems = matchingRepository.getDashboardItems(
                freelancerMember.getEmail().getValue(), "IN_PROGRESS", null);
        List<FreelancerDashboardItem> completedItems = matchingRepository.getDashboardItems(
                freelancerMember.getEmail().getValue(), "COMPLETED", null);

        assertThat(newItems).extracting(FreelancerDashboardItem::matchingId)
                .containsExactly(proposedMatching.getId());
        assertThat(inProgressItems).extracting(FreelancerDashboardItem::matchingId)
                .containsExactly(acceptedMatching.getId());
        assertThat(completedItems).extracting(FreelancerDashboardItem::matchingId)
                .containsExactly(completedMatching.getId());
    }

    private Matching saveMatching(ProposalPosition proposalPosition, Resume resume, MatchingStatus status) {
        Matching matching = Matching.create(
                resume,
                proposalPosition,
                clientMember,
                resume.getMember()
        );
        ReflectionTestUtils.setField(matching, "status", status);
        return matchingRepository.saveAndFlush(matching);
    }

    private Position persistPosition(String name) {
        Position position = Position.create(name);
        em.persist(position);
        return position;
    }

    private ProposalPosition saveAdditionalPosition(String proposalTitle, String positionName) {
        Position position = persistPosition(positionName);
        Proposal extraProposal = Proposal.create(clientMember, proposalTitle, "원문", null, null, null, null);
        ProposalPosition extraPosition = extraProposal.addPosition(
                position,
                positionName,
                null,
                1L,
                2_000_000L,
                4_000_000L,
                null,
                null,
                null,
                null
        );
        proposalRepository.saveAndFlush(extraProposal);
        return extraPosition;
    }
}
