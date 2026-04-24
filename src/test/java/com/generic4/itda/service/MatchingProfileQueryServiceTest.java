package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingCancellationReason;
import com.generic4.itda.domain.matching.constant.MatchingParticipantRole;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.member.UserType;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.resume.CareerEmploymentType;
import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.profile.ProfileAccessLevel;
import com.generic4.itda.dto.profile.ProfileContextType;
import com.generic4.itda.dto.profile.ProfileShellViewModel;
import com.generic4.itda.dto.profile.ProfileSubjectType;
import com.generic4.itda.repository.MatchingRepository;
import java.util.List;
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
class MatchingProfileQueryServiceTest {

    private static final String CLIENT_EMAIL = "client@example.com";
    private static final String FREELANCER_EMAIL = "freelancer@example.com";

    @Mock
    private MatchingRepository matchingRepository;

    @InjectMocks
    private MatchingProfileQueryService matchingProfileQueryService;

    @Test
    @DisplayName("클라이언트가 제안 단계 매칭을 조회하면 프리랜서 프로필을 마스킹된 미리보기로 반환한다")
    void getCounterpartProfile_returnsMaskedFreelancerPreviewForClientOnProposed() {
        Matching matching = createProposedMatching();
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        ProfileShellViewModel result = matchingProfileQueryService.getCounterpartProfile(401L, CLIENT_EMAIL);

        assertThat(result.subjectType()).isEqualTo(ProfileSubjectType.FREELANCER);
        assertThat(result.contextType()).isEqualTo(ProfileContextType.MATCHING);
        assertThat(result.accessLevel()).isEqualTo(ProfileAccessLevel.PREVIEW);
        assertThat(result.title()).isEqualTo("길*");
        assertThat(result.subtitle()).isEqualTo("AI 매칭 플랫폼 · 플랫폼 백엔드 개발자");
        assertThat(result.statusLabel()).isEqualTo("제안됨");
        assertThat(result.backUrl()).isEqualTo("/matchings/401");
        assertThat(result.projectSummary()).isNotNull();
        assertThat(result.projectSummary().proposalTitle()).isEqualTo("AI 매칭 플랫폼");
        assertThat(result.projectSummary().positionTitle()).isEqualTo("플랫폼 백엔드 개발자");
        assertThat(result.projectSummary().essentialSkills()).containsExactly("Java");
        assertThat(result.client()).isNull();
        assertThat(result.freelancer()).isNotNull();
        assertThat(result.freelancer().displayName()).isEqualTo("길*");
        assertThat(result.freelancer().headline()).isEqualTo("프리랜서 프로필");
        assertThat(result.freelancer().introduction()).isEqualTo("커머스와 금융 도메인 백엔드 개발 경험이 있습니다.");
        assertThat(result.freelancer().careerYears()).isEqualTo(5);
        assertThat(result.freelancer().preferredWorkTypeLabel()).isEqualTo("상주, 원격 모두 가능");
        assertThat(result.freelancer().portfolioUrl()).isEqualTo("https://portfolio.example.com");
        assertThat(result.freelancer().skills())
                .extracting("name", "proficiencyLabel", "proficiencyCode")
                .containsExactly(
                        tuple("Java", "고급", "ADVANCED"),
                        tuple("Spring", "중급", "INTERMEDIATE")
                );
        assertThat(result.freelancer().careerItems()).singleElement().satisfies(item -> {
            assertThat(item.companyName()).isEqualTo("네오핀");
            assertThat(item.position()).isEqualTo("백엔드 개발자");
            assertThat(item.employmentTypeLabel()).isEqualTo("정규직");
            assertThat(item.periodLabel()).isEqualTo("2021-01 ~ 2023-06");
            assertThat(item.summary()).isEqualTo("결제 API와 정산 배치를 개발했습니다.");
            assertThat(item.techStack()).containsExactly("Java", "Spring Boot", "MySQL");
        });
        assertThat(result.hasMatchingContext()).isTrue();
        assertThat(result.hasRecommendationContext()).isFalse();
        assertThat(result.matchingContext()).isNotNull();
        assertThat(result.matchingContext().viewerRole()).isEqualTo("CLIENT");
        assertThat(result.matchingContext().matchingStatus()).isEqualTo("PROPOSED");
        assertThat(result.matchingContext().contactVisible()).isFalse();
        assertThat(result.matchingContext().contactEmail()).isNull();
        assertThat(result.matchingContext().contactPhone()).isNull();
        assertThat(result.matchingContext().statusLabel()).isEqualTo("제안됨");
        assertThat(result.matchingContext().helperMessage())
                .isEqualTo("프리랜서가 요청을 확인하고 응답하면 다음 단계로 넘어갈 수 있습니다.");
        assertThat(result.matchingContext().matchingDetailUrl()).isEqualTo("/matchings/401");
        assertThat(result.matchingContext().proposalDetailUrl()).isEqualTo("/proposals/200");
        assertThat(result.matchingContext().acceptActionUrl()).isEqualTo("/matchings/401/accept");
        assertThat(result.matchingContext().rejectActionUrl()).isEqualTo("/matchings/401/reject");
        assertThat(result.matchingContext().canRespond()).isFalse();

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("프리랜서가 제안 단계 매칭을 조회하면 클라이언트 프로필과 응답 가능 상태를 반환한다")
    void getCounterpartProfile_returnsMaskedClientPreviewAndResponseStateForFreelancerOnProposed() {
        Matching matching = createProposedMatching();
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        ProfileShellViewModel result = matchingProfileQueryService.getCounterpartProfile(401L, FREELANCER_EMAIL);

        assertThat(result.subjectType()).isEqualTo(ProfileSubjectType.CLIENT);
        assertThat(result.accessLevel()).isEqualTo(ProfileAccessLevel.PREVIEW);
        assertThat(result.title()).isEqualTo("에***A");
        assertThat(result.projectSummary()).isNotNull();
        assertThat(result.freelancer()).isNull();
        assertThat(result.client()).isNotNull();
        assertThat(result.client().displayName()).isEqualTo("에***A");
        assertThat(result.client().userTypeLabel()).isEqualTo("기업");
        assertThat(result.client().memo()).isEqualTo("금융/커머스 프로젝트 경험자를 선호합니다.");
        assertThat(result.client().project().proposalId()).isEqualTo(200L);
        assertThat(result.client().project().proposalTitle()).isEqualTo("AI 매칭 플랫폼");
        assertThat(result.client().project().description()).isEqualTo("AI 추천 기반 외주 매칭 프로젝트");
        assertThat(result.client().project().positionTitle()).isEqualTo("플랫폼 백엔드 개발자");
        assertThat(result.client().project().workTypeLabel()).isEqualTo("원격");
        assertThat(result.client().project().budgetText()).isEqualTo("3,000,000원 ~ 5,000,000원");
        assertThat(result.client().project().expectedPeriodText()).isEqualTo("4주");
        assertThat(result.client().project().essentialSkills()).containsExactly("Java");
        assertThat(result.client().project().preferredSkills()).containsExactly("Docker");
        assertThat(result.matchingContext().viewerRole()).isEqualTo("FREELANCER");
        assertThat(result.matchingContext().contactVisible()).isFalse();
        assertThat(result.matchingContext().contactEmail()).isNull();
        assertThat(result.matchingContext().contactPhone()).isNull();
        assertThat(result.matchingContext().helperMessage()).isEqualTo("요청 내용을 확인한 뒤 수락 또는 거절할 수 있습니다.");
        assertThat(result.matchingContext().canRespond()).isTrue();

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("프리랜서가 수락된 매칭을 조회하면 클라이언트 프로필은 실명 공개 상태로 반환된다")
    void getCounterpartProfile_returnsFullClientProfileWhenContactIsVisible() {
        Matching matching = createAcceptedMatching();
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        ProfileShellViewModel result = matchingProfileQueryService.getCounterpartProfile(401L, FREELANCER_EMAIL);

        assertThat(result.subjectType()).isEqualTo(ProfileSubjectType.CLIENT);
        assertThat(result.accessLevel()).isEqualTo(ProfileAccessLevel.FULL);
        assertThat(result.title()).isEqualTo("에이전시A");
        assertThat(result.client().displayName()).isEqualTo("에이전시A");
        assertThat(result.client().userTypeLabel()).isEqualTo("기업");
        assertThat(result.matchingContext().matchingStatus()).isEqualTo("ACCEPTED");
        assertThat(result.matchingContext().contactVisible()).isTrue();
        assertThat(result.matchingContext().contactEmail()).isEqualTo(CLIENT_EMAIL);
        assertThat(result.matchingContext().contactPhone()).isEqualTo("010-0000-0001");
        assertThat(result.matchingContext().hasContactDetails()).isTrue();
        assertThat(result.matchingContext().helperMessage())
                .isEqualTo("연락처가 공개되었습니다. 제안서를 다시 확인하고 협의를 이어가세요.");
        assertThat(result.matchingContext().canRespond()).isFalse();

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("계약 이후 취소된 매칭은 취소 상태여도 연락처 공개 레벨을 유지한다")
    void getCounterpartProfile_keepsFullAccessForCanceledContract() {
        Matching matching = createContractCanceledMatching();
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        ProfileShellViewModel result = matchingProfileQueryService.getCounterpartProfile(401L, CLIENT_EMAIL);

        assertThat(result.subjectType()).isEqualTo(ProfileSubjectType.FREELANCER);
        assertThat(result.accessLevel()).isEqualTo(ProfileAccessLevel.FULL);
        assertThat(result.title()).isEqualTo("길동");
        assertThat(result.statusLabel()).isEqualTo("취소됨");
        assertThat(result.matchingContext().matchingStatus()).isEqualTo("CANCELED");
        assertThat(result.matchingContext().contactVisible()).isTrue();
        assertThat(result.matchingContext().contactEmail()).isEqualTo(FREELANCER_EMAIL);
        assertThat(result.matchingContext().contactPhone()).isEqualTo("010-0000-0002");
        assertThat(result.matchingContext().hasContactDetails()).isTrue();
        assertThat(result.matchingContext().helperMessage())
                .isEqualTo("취소된 계약입니다. 필요한 경우 진행 이력과 연락처 정보를 확인해보세요.");

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("매칭 당사자가 아니면 상대 프로필을 조회할 수 없다")
    void getCounterpartProfile_throwsWhenViewerIsNotParticipant() {
        Matching matching = createAcceptedMatching();
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.of(matching));

        assertThatThrownBy(() -> matchingProfileQueryService.getCounterpartProfile(401L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 매칭 정보에 접근할 수 없습니다.");

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    @Test
    @DisplayName("매칭이 없으면 예외를 던진다")
    void getCounterpartProfile_throwsWhenMatchingDoesNotExist() {
        given(matchingRepository.findDetailById(401L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> matchingProfileQueryService.getCounterpartProfile(401L, CLIENT_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("매칭 정보를 찾을 수 없습니다. id=401");

        verify(matchingRepository).findDetailById(401L);
        verifyNoMoreInteractions(matchingRepository);
    }

    private Matching createAcceptedMatching() {
        Matching matching = createProposedMatching();
        matching.accept();
        return matching;
    }

    private Matching createContractCanceledMatching() {
        Matching matching = createAcceptedMatching();
        matching.acceptContractStart(MatchingParticipantRole.CLIENT);
        matching.acceptContractStart(MatchingParticipantRole.FREELANCER);
        matching.requestCancellation(
                MatchingParticipantRole.CLIENT,
                MatchingCancellationReason.CLIENT_AFTER_PROJECT_SUSPENDED,
                null
        );
        matching.confirmCancellation(MatchingParticipantRole.FREELANCER);
        return matching;
    }

    private Matching createProposedMatching() {
        Member client = createMember(
                CLIENT_EMAIL,
                "hashed-password",
                "주식회사 에이전시",
                "에이전시A",
                "금융/커머스 프로젝트 경험자를 선호합니다.",
                "010-0000-0001"
        );
        ReflectionTestUtils.setField(client, "type", UserType.CORPORATE);

        Member freelancer = createMember(
                FREELANCER_EMAIL,
                "hashed-password",
                "홍길동",
                "길동",
                "010-0000-0002"
        );

        Position position = Position.create("백엔드 개발자");
        ReflectionTestUtils.setField(position, "id", 101L);

        Skill java = Skill.create("Java", null);
        ReflectionTestUtils.setField(java, "id", 1001L);

        Skill spring = Skill.create("Spring", null);
        ReflectionTestUtils.setField(spring, "id", 1002L);

        Skill docker = Skill.create("Docker", null);
        ReflectionTestUtils.setField(docker, "id", 1003L);

        Proposal proposal = Proposal.create(
                client,
                "AI 매칭 플랫폼",
                "원본 입력",
                "AI 추천 기반 외주 매칭 프로젝트",
                6_000_000L,
                10_000_000L,
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
                3,
                8,
                null
        );
        proposalPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        proposalPosition.addSkill(docker, ProposalPositionSkillImportance.PREFERENCE);
        ReflectionTestUtils.setField(proposalPosition, "id", 201L);

        Resume resume = Resume.create(
                freelancer,
                "커머스와 금융 도메인 백엔드 개발 경험이 있습니다.",
                (byte) 5,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://portfolio.example.com"
        );
        resume.addSkill(java, Proficiency.ADVANCED);
        resume.addSkill(spring, Proficiency.INTERMEDIATE);
        ReflectionTestUtils.setField(resume, "id", 301L);

        Matching matching = Matching.create(resume, proposalPosition, client, freelancer);
        ReflectionTestUtils.setField(matching, "id", 401L);
        return matching;
    }

    private CareerPayload createCareerPayload() {
        CareerItemPayload careerItem = new CareerItemPayload();
        careerItem.setCompanyName("네오핀");
        careerItem.setPosition("백엔드 개발자");
        careerItem.setEmploymentType(CareerEmploymentType.FULL_TIME);
        careerItem.setStartYearMonth("2021-01");
        careerItem.setEndYearMonth("2023-06");
        careerItem.setCurrentlyWorking(false);
        careerItem.setSummary("결제 API와 정산 배치를 개발했습니다.");
        careerItem.setTechStack(List.of("Java", "Spring Boot", "MySQL"));

        CareerPayload payload = new CareerPayload();
        payload.setItems(List.of(careerItem));
        return payload;
    }
}
