package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingCancellationReason;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.matching.MatchingDetailCancellationViewModel;
import com.generic4.itda.dto.matching.MatchingDetailContactViewModel;
import com.generic4.itda.dto.matching.MatchingDetailHeaderViewModel;
import com.generic4.itda.dto.matching.MatchingDetailLifecycleViewModel;
import com.generic4.itda.dto.matching.MatchingDetailProjectSummaryViewModel;
import com.generic4.itda.dto.matching.MatchingDetailSummaryViewModel;
import com.generic4.itda.dto.matching.MatchingDetailViewModel;
import com.generic4.itda.dto.matching.MatchingParticipantContactViewModel;
import com.generic4.itda.dto.matching.MatchingTimelineItemViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.MatchingQueryService;
import com.generic4.itda.service.MatchingService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(MatchingController.class)
class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchingService matchingService;

    @MockitoBean
    private MatchingQueryService matchingQueryService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("매칭 상세 페이지를 렌더링한다")
    void renderMatchingDetailPage() throws Exception {
        given(matchingQueryService.getDetail(401L, "client@example.com"))
                .willReturn(createMatchingDetailView());

        mockMvc.perform(get("/matchings/401")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("matching/detail"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("매칭 프로세스 현황")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("연락처 정보")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("프로젝트 요약")));
    }

    @Test
    @DisplayName("없는 매칭 상세를 조회하면 홈으로 이동하며 오류 메시지를 남긴다")
    void redirectHomeWhenMatchingDetailNotFound() throws Exception {
        given(matchingQueryService.getDetail(999L, "client@example.com"))
                .willThrow(new IllegalArgumentException("매칭 정보를 찾을 수 없습니다. id=999"));

        mockMvc.perform(get("/matchings/999")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", "존재하지 않는 매칭입니다."));
    }

    @Test
    @DisplayName("권한 없는 매칭 상세를 조회하면 홈으로 이동하며 오류 메시지를 남긴다")
    void redirectHomeWhenMatchingDetailAccessDenied() throws Exception {
        given(matchingQueryService.getDetail(401L, "client@example.com"))
                .willThrow(new AccessDeniedException("해당 매칭 정보에 접근할 수 없습니다."));

        mockMvc.perform(get("/matchings/401")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", "해당 매칭 정보에 접근할 수 없습니다."));
    }

    @Test
    @DisplayName("클라이언트가 추천 결과로 매칭 요청을 보내면 지정한 화면으로 돌아간다")
    void requestMatchingAndRedirectToProvidedPath() throws Exception {
        Matching matching = createMatching();
        given(matchingService.request(101L, "client@example.com")).willReturn(matching);

        mockMvc.perform(post("/recommendation-results/101/matchings")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/recommendation-results/101/matchings")
                        .with(authentication(authToken("client@example.com", "클라이언트")))
                        .with(csrf())
                        .param("redirectTo", "/proposals/200/recommendations/results?runId=501"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/200/recommendations/results?runId=501"))
                .andExpect(flash().attribute("noticeMessage", "매칭 요청을 보냈습니다."));

        then(matchingService).should().request(101L, "client@example.com");
    }

    @Test
    @DisplayName("클라이언트가 추천 결과로 매칭 요청을 보내면 기본적으로 매칭 상세로 이동한다")
    void requestMatchingAndRedirectToMatchingDetailByDefault() throws Exception {
        Matching matching = createMatching();
        given(matchingService.request(101L, "client@example.com")).willReturn(matching);

        mockMvc.perform(post("/recommendation-results/101/matchings")
                        .with(authentication(authToken("client@example.com", "클라이언트")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("noticeMessage", "매칭 요청을 보냈습니다."));
    }

    @Test
    @DisplayName("매칭 요청 생성에 실패하면 안전한 경로로 리다이렉트하며 오류 메시지를 남긴다")
    void redirectWithErrorWhenRequestMatchingFails() throws Exception {
        given(matchingService.request(101L, "client@example.com"))
                .willThrow(new IllegalStateException("이미 요청했거나 진행 중인 매칭입니다."));

        mockMvc.perform(post("/recommendation-results/101/matchings")
                        .with(authentication(authToken("client@example.com", "클라이언트")))
                        .with(csrf())
                        .param("errorRedirectTo", "/proposals/200/recommendations/results?runId=501"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/200/recommendations/results?runId=501"))
                .andExpect(flash().attribute("errorMessage", "이미 요청했거나 진행 중인 후보입니다."));
    }

    @Test
    @DisplayName("프리랜서가 수락하면 기본적으로 매칭 상세로 이동한다")
    void acceptMatchingAndRedirectToMatchingDetail() throws Exception {
        Matching matching = createMatching();
        given(matchingService.accept(401L, "freelancer@example.com")).willReturn(matching);

        mockMvc.perform(post("/matchings/401/accept")
                        .with(authentication(authToken("freelancer@example.com", "프리랜서")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("noticeMessage", "매칭 요청을 수락했습니다."));

        then(matchingService).should().accept(401L, "freelancer@example.com");
    }

    @Test
    @DisplayName("프리랜서가 거절하면 지정한 화면으로 돌아간다")
    void rejectMatchingAndRedirectToProvidedPath() throws Exception {
        Matching matching = createMatching();
        given(matchingService.reject(401L, "freelancer@example.com")).willReturn(matching);

        mockMvc.perform(post("/matchings/401/reject")
                        .with(authentication(authToken("freelancer@example.com", "프리랜서")))
                        .with(csrf())
                        .param("redirectTo", "/proposals/200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/200"))
                .andExpect(flash().attribute("noticeMessage", "매칭 요청을 거절했습니다."));

        then(matchingService).should().reject(401L, "freelancer@example.com");
    }

    @Test
    @DisplayName("없는 매칭 요청에 응답하면 프리랜서 대시보드로 돌아간다")
    void redirectDashboardWhenMatchingNotFound() throws Exception {
        given(matchingService.accept(999L, "freelancer@example.com"))
                .willThrow(new IllegalArgumentException("매칭 요청을 찾을 수 없습니다. id=999"));

        mockMvc.perform(post("/matchings/999/accept")
                        .with(authentication(authToken("freelancer@example.com", "프리랜서")))
                        .with(csrf())
                        .param("errorRedirectTo", "https://example.com/outside"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/freelancers/dashboard"))
                .andExpect(flash().attribute("errorMessage", "응답할 매칭 요청을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("본인에게 온 요청이 아니면 오류 메시지와 함께 프리랜서 대시보드로 돌아간다")
    void redirectDashboardWhenMatchingAccessDenied() throws Exception {
        given(matchingService.reject(401L, "freelancer@example.com"))
                .willThrow(new AccessDeniedException("본인에게 온 매칭 요청에만 응답할 수 있습니다."));

        mockMvc.perform(post("/matchings/401/reject")
                        .with(authentication(authToken("freelancer@example.com", "프리랜서")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/freelancers/dashboard"))
                .andExpect(flash().attribute("errorMessage", "본인에게 온 매칭 요청에만 응답할 수 있습니다."));
    }

    @Test
    @DisplayName("정원이 찬 포지션 수락 실패는 사용자용 메시지로 변환한다")
    void redirectDashboardWithMappedMessageWhenCapacityFull() throws Exception {
        given(matchingService.accept(401L, "freelancer@example.com"))
                .willThrow(new IllegalStateException("정원이 이미 찬 모집 포지션입니다."));

        mockMvc.perform(post("/matchings/401/accept")
                        .with(authentication(authToken("freelancer@example.com", "프리랜서")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/freelancers/dashboard"))
                .andExpect(flash().attribute("errorMessage", "이미 정원이 마감된 포지션입니다."));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 매칭 응답 시 로그인 페이지로 이동한다")
    void redirectToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/matchings/401/accept").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("계약 시작 확인을 저장하면 매칭 상세로 이동한다")
    void acceptContractStartAndRedirectToMatchingDetail() throws Exception {
        Matching matching = createMatching();
        given(matchingService.acceptContractStart(401L, "client@example.com")).willReturn(matching);

        mockMvc.perform(post("/matchings/401/contract-start/accept")
                        .with(authentication(authToken("client@example.com", "클라이언트")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("noticeMessage", "계약 시작 확인을 저장했습니다."));

        then(matchingService).should().acceptContractStart(401L, "client@example.com");
    }

    @Test
    @DisplayName("취소 요청 폼을 서비스로 전달한다")
    void requestCancellationAndRedirectToMatchingDetail() throws Exception {
        Matching matching = createMatching();
        given(matchingService.requestCancellation(
                401L,
                "client@example.com",
                MatchingCancellationReason.CLIENT_BEFORE_OTHER,
                "내부 일정 변경"
        )).willReturn(matching);

        mockMvc.perform(post("/matchings/401/cancellations")
                        .with(authentication(authToken("client@example.com", "클라이언트")))
                        .with(csrf())
                        .param("reason", "CLIENT_BEFORE_OTHER")
                        .param("reasonDetail", "내부 일정 변경"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("noticeMessage", "취소 요청을 보냈습니다."));

        then(matchingService).should().requestCancellation(
                401L,
                "client@example.com",
                MatchingCancellationReason.CLIENT_BEFORE_OTHER,
                "내부 일정 변경"
        );
    }

    @Test
    @DisplayName("취소 요청 철회를 처리한다")
    void withdrawCancellationAndRedirectToMatchingDetail() throws Exception {
        Matching matching = createMatching();
        given(matchingService.withdrawCancellation(401L, "client@example.com")).willReturn(matching);

        mockMvc.perform(post("/matchings/401/cancellations/withdraw")
                        .with(authentication(authToken("client@example.com", "클라이언트")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("noticeMessage", "취소 요청을 철회했습니다."));

        then(matchingService).should().withdrawCancellation(401L, "client@example.com");
    }

    @Test
    @DisplayName("취소 요청 확인을 처리한다")
    void confirmCancellationAndRedirectToMatchingDetail() throws Exception {
        Matching matching = createMatching();
        given(matchingService.confirmCancellation(401L, "freelancer@example.com")).willReturn(matching);

        mockMvc.perform(post("/matchings/401/cancellations/confirm")
                        .with(authentication(authToken("freelancer@example.com", "프리랜서")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("noticeMessage", "취소 요청을 확인하여 매칭이 취소되었습니다."));

        then(matchingService).should().confirmCancellation(401L, "freelancer@example.com");
    }

    @Test
    @DisplayName("후기 작성 내용을 서비스로 전달한다")
    void submitReviewAndRedirectToMatchingDetail() throws Exception {
        Matching matching = createMatching();
        given(matchingService.submitReview(401L, "client@example.com", "좋은 협업이었습니다."))
                .willReturn(matching);

        mockMvc.perform(post("/matchings/401/reviews")
                        .with(authentication(authToken("client@example.com", "클라이언트")))
                        .with(csrf())
                        .param("review", "좋은 협업이었습니다."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("noticeMessage", "후기를 저장했습니다."));

        then(matchingService).should().submitReview(401L, "client@example.com", "좋은 협업이었습니다.");
    }

    @Test
    @DisplayName("완료 확인을 저장하면 매칭 상세로 이동한다")
    void confirmCompletionAndRedirectToMatchingDetail() throws Exception {
        Matching matching = createMatching();
        given(matchingService.confirmCompletion(401L, "client@example.com")).willReturn(matching);

        mockMvc.perform(post("/matchings/401/completion/confirm")
                        .with(authentication(authToken("client@example.com", "클라이언트")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("noticeMessage", "프로젝트 완료 확인을 저장했습니다."));

        then(matchingService).should().confirmCompletion(401L, "client@example.com");
    }

    @Test
    @DisplayName("상태 전이 요청 실패는 사용자용 메시지로 변환한다")
    void redirectWithMappedMessageWhenLifecycleActionFails() throws Exception {
        given(matchingService.requestCancellation(
                401L,
                "client@example.com",
                MatchingCancellationReason.CLIENT_BEFORE_OTHER,
                null
        )).willThrow(new IllegalArgumentException("기타 취소 사유를 입력해주세요."));

        mockMvc.perform(post("/matchings/401/cancellations")
                        .with(authentication(authToken("client@example.com", "클라이언트")))
                        .with(csrf())
                        .param("reason", "CLIENT_BEFORE_OTHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("errorMessage", "기타 사유를 선택한 경우 상세 사유를 입력해주세요."));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 매칭 상세 조회 시 로그인 페이지로 이동한다")
    void redirectToLoginWhenUnauthenticatedOnDetail() throws Exception {
        mockMvc.perform(get("/matchings/401"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    private UsernamePasswordAuthenticationToken authToken(String email, String name) {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember(email, "hashed-password", name, "010-1234-5678")
        );
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private Matching createMatching() {
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

        ReflectionTestUtils.setField(proposal, "id", 200L);
        ReflectionTestUtils.setField(proposalPosition, "id", 201L);

        Matching matching = Matching.create(null, proposalPosition, client, freelancer);
        ReflectionTestUtils.setField(matching, "id", 401L);
        return matching;
    }

    private MatchingDetailViewModel createMatchingDetailView() {
        return new MatchingDetailViewModel(
                401L,
                "CLIENT",
                MatchingStatus.PROPOSED,
                false,
                new MatchingDetailHeaderViewModel("AI 매칭 플랫폼", "플랫폼 백엔드 개발자", "김프리랜서", "매칭 요청"),
                new MatchingDetailSummaryViewModel(
                        "프리랜서 응답을 기다리는 중입니다.",
                        "프리랜서가 요청을 확인하고 응답하면 다음 단계로 넘어갈 수 있습니다.",
                        LocalDateTime.of(2026, 4, 22, 13, 0),
                        null,
                        "매칭이 수락되기 전까지는 연락처가 공개되지 않습니다."
                ),
                new MatchingDetailContactViewModel(
                        false,
                        new MatchingParticipantContactViewModel("클라이언트", "클라이언트", "client@example.com", "010-1111-2222"),
                        new MatchingParticipantContactViewModel("프리랜서", "김프리랜서", "freelancer@example.com", "010-3333-4444")
                ),
                new MatchingDetailProjectSummaryViewModel(
                        200L,
                        "AI 매칭 플랫폼",
                        "설명",
                        "플랫폼 백엔드 개발자",
                        "백엔드 개발자",
                        "3,000,000원 ~ 5,000,000원",
                        "4주",
                        "원격",
                        List.of("Java", "Spring"),
                        List.of("Docker")
                ),
                List.of(
                        new MatchingTimelineItemViewModel(
                                LocalDateTime.of(2026, 4, 22, 13, 0),
                                "클라이언트",
                                "매칭 요청 전송",
                                "프리랜서에게 매칭 요청을 보냈습니다."
                        )
                ),
                new MatchingDetailLifecycleViewModel(
                        "클라이언트",
                        "프리랜서",
                        false,
                        false,
                        false,
                        false,
                        false,
                        new MatchingDetailCancellationViewModel(
                                false,
                                false,
                                false,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        ),
                        false,
                        false,
                        false,
                        false,
                        false,
                        null,
                        null,
                        false,
                        false,
                        false,
                        false
                )
        );
    }
}
