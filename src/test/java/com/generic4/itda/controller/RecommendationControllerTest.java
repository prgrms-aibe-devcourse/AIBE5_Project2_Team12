package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.dto.recommend.RecommendationCandidateItem;
import com.generic4.itda.dto.recommend.RecommendationResumeCareerItem;
import com.generic4.itda.dto.recommend.RecommendationResumeDetailViewModel;
import com.generic4.itda.dto.recommend.RecommendationResumeSkillItem;
import com.generic4.itda.dto.recommend.RecommendationResultsViewModel;
import com.generic4.itda.dto.recommend.RecommendationRunStatusViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.recommend.RecommendationRunQueryService;
import com.generic4.itda.service.recommend.RecommendationRunService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(RecommendationController.class)
class RecommendationControllerTest {

    private static final Long PROPOSAL_ID = 10L;
    private static final Long PROPOSAL_POSITION_ID = 20L;
    private static final Long RUN_ID = 301L;
    private static final Long RESULT_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationRunService recommendationRunService;

    @MockitoBean
    private RecommendationRunQueryService recommendationRunQueryService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("GET /recommendations — 제안서 상세 모달로 redirect한다")
    void entryRedirectsToProposalDetail() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        // when / then
        mockMvc.perform(get("/proposals/{proposalId}/recommendations", PROPOSAL_ID)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/" + PROPOSAL_ID + "?openRecommendModal=true"));
    }

    @Test
    @DisplayName("추천 결과 조회가 성공하면 results 화면을 렌더링한다")
    void renderResults() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        RecommendationResultsViewModel viewModel = new RecommendationResultsViewModel(
                PROPOSAL_ID,
                PROPOSAL_POSITION_ID,
                RUN_ID,
                "추천 테스트 제안서",
                "백엔드 개발자",
                3,
                3,
                List.of(new RecommendationCandidateItem(
                        100L,
                        1,
                        "닉*임",
                        "",
                        5,
                        "원격",
                        95,
                        88,
                        List.of("Java"),
                        List.of("대규모 트래픽 처리 경험"),
                        null,
                        "대기 중",
                        false,
                        null,  // matchingId — 매칭 없음
                        null   // matchingStatus — 매칭 없음
                ))
        );

        given(recommendationRunQueryService.getRecommendationResults(eq(PROPOSAL_ID), eq(RUN_ID), eq("client@example.com")))
                .willReturn(viewModel);

        // when / then
        mockMvc.perform(get("/proposals/{proposalId}/recommendations/results", PROPOSAL_ID)
                        .param("runId", String.valueOf(RUN_ID))
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation/results"))
                .andExpect(model().attributeExists("view"));

        then(recommendationRunQueryService).should()
                .getRecommendationResults(eq(PROPOSAL_ID), eq(RUN_ID), eq("client@example.com"));
    }

    @Test
    @DisplayName("추천 결과가 아직 준비되지 않으면 error 화면을 렌더링한다")
    void renderErrorWhenNotReady() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        given(recommendationRunQueryService.getRecommendationResults(eq(PROPOSAL_ID), eq(RUN_ID), eq("client@example.com")))
                .willThrow(new IllegalStateException("추천 결과가 아직 준비되지 않았습니다."));

        // when / then
        mockMvc.perform(get("/proposals/{proposalId}/recommendations/results", PROPOSAL_ID)
                        .param("runId", String.valueOf(RUN_ID))
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation/error"))
                .andExpect(model().attribute("title", "추천 결과를 확인할 수 없습니다."))
                .andExpect(model().attribute("message", "추천이 아직 완료되지 않았습니다. 잠시 후 다시 시도해주세요."))
                .andExpect(model().attribute("backUrl", "/proposals/10/runs/301"));

        then(recommendationRunQueryService).should()
                .getRecommendationResults(eq(PROPOSAL_ID), eq(RUN_ID), eq("client@example.com"));
    }

    @Test
    @DisplayName("추천 후보 이력서 조회가 성공하면 resume 화면을 렌더링한다")
    void renderCandidateResume() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        RecommendationResumeDetailViewModel viewModel = new RecommendationResumeDetailViewModel(
                PROPOSAL_ID,
                RUN_ID,
                RESULT_ID,
                "제안서",
                "백엔드 개발자",
                "/proposals/10/recommendations/results?runId=301",
                new RecommendationCandidateItem(
                        RESULT_ID,
                        1,
                        "김*수",
                        "소개",
                        5,
                        "원격",
                        95,
                        88,
                        List.of("Java"),
                        List.of("강점"),
                        "사유",
                        "READY",
                        true,
                        9001L,       // matchingId
                        "PROPOSED"   // matchingStatus
                ),
                "https://example.com",
                List.of(new RecommendationResumeSkillItem("Java", "고급", "ADVANCED")),
                List.of(new RecommendationResumeCareerItem(
                        "ACME",
                        "Backend",
                        "정규직",
                        "2024-01 ~ 2025-12",
                        "요약",
                        List.of("Java")
                ))
        );

        given(recommendationRunQueryService.getRecommendationCandidateResume(eq(PROPOSAL_ID), eq(RESULT_ID), eq("client@example.com")))
                .willReturn(viewModel);

        // when / then
        mockMvc.perform(get("/proposals/{proposalId}/recommendations/results/{resultId}", PROPOSAL_ID, RESULT_ID)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation/resume"))
                .andExpect(model().attributeExists("view"))
                .andExpect(content().string(containsString("text-amber-600")));

        then(recommendationRunQueryService).should()
                .getRecommendationCandidateResume(eq(PROPOSAL_ID), eq(RESULT_ID), eq("client@example.com"));
    }

    @Test
    @DisplayName("추천 후보 이력서 조회 실패 시 error 화면을 렌더링한다")
    void renderErrorWhenCandidateResumeFails() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        given(recommendationRunQueryService.getRecommendationCandidateResume(eq(PROPOSAL_ID), eq(RESULT_ID), eq("client@example.com")))
                .willThrow(new IllegalArgumentException("추천 결과를 찾을 수 없습니다."));

        // when / then
        mockMvc.perform(get("/proposals/{proposalId}/recommendations/results/{resultId}", PROPOSAL_ID, RESULT_ID)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation/error"))
                .andExpect(model().attribute("title", "추천 후보 이력서를 확인할 수 없습니다."))
                .andExpect(model().attribute("backUrl", "/proposals/" + PROPOSAL_ID + "?openRecommendModal=true"));
    }

    // ── GET /proposals/{proposalId}/runs/{runId} ─────────────────────────────

    @Test
    @DisplayName("추천 실행 상태 조회가 성공하면 status 화면을 렌더링한다")
    void renderRunStatus() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        RecommendationRunStatusViewModel statusView = new RecommendationRunStatusViewModel(
                PROPOSAL_ID, RUN_ID, "추천 테스트 제안서",
                RecommendationRunStatus.PENDING,
                "추천 결과를 생성하고 있습니다.",
                "잠시 후 자동으로 결과를 확인할 수 있습니다.",
                "새로고침", null, true
        );

        given(recommendationRunQueryService.getRecommendationRunStatus(eq(PROPOSAL_ID), eq(RUN_ID), eq("client@example.com")))
                .willReturn(statusView);

        // when / then
        mockMvc.perform(get("/proposals/{proposalId}/runs/{runId}", PROPOSAL_ID, RUN_ID)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation/status"))
                .andExpect(model().attributeExists("view"));

        then(recommendationRunQueryService).should()
                .getRecommendationRunStatus(eq(PROPOSAL_ID), eq(RUN_ID), eq("client@example.com"));
    }

    @Test
    @DisplayName("추천 실행 정보를 찾을 수 없으면 error 화면을 렌더링한다")
    void renderErrorWhenRunNotFound() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        given(recommendationRunQueryService.getRecommendationRunStatus(eq(PROPOSAL_ID), eq(RUN_ID), eq("client@example.com")))
                .willThrow(new IllegalArgumentException("추천 실행 정보를 찾을 수 없습니다."));

        // when / then
        mockMvc.perform(get("/proposals/{proposalId}/runs/{runId}", PROPOSAL_ID, RUN_ID)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation/error"))
                .andExpect(model().attribute("title", "추천 실행 정보를 확인할 수 없습니다."))
                .andExpect(model().attribute("message", "존재하지 않거나 만료된 추천 실행입니다."))
                .andExpect(model().attribute("backUrl", "/proposals/" + PROPOSAL_ID + "?openRecommendModal=true"));
    }

    @Test
    @DisplayName("추천 실행 접근 권한이 없으면 error 화면에 권한 없음 메시지를 렌더링한다")
    void renderErrorWhenRunAccessDenied() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        given(recommendationRunQueryService.getRecommendationRunStatus(eq(PROPOSAL_ID), eq(RUN_ID), eq("client@example.com")))
                .willThrow(new IllegalArgumentException("접근 권한이 없습니다."));

        // when / then
        mockMvc.perform(get("/proposals/{proposalId}/runs/{runId}", PROPOSAL_ID, RUN_ID)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("recommendation/error"))
                .andExpect(model().attribute("message", "해당 추천 실행 정보에 접근할 수 없습니다."));
    }

    // ── POST /proposals/{proposalId}/recommendations (AJAX — Accept: application/json) ──

    @Test
    @DisplayName("추천 시작 AJAX 요청이 성공하면 JSON으로 redirect URL을 반환한다")
    void runAjaxReturnsRedirectUrl() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        given(recommendationRunService.createOrReuse(eq(PROPOSAL_ID), eq(PROPOSAL_POSITION_ID), eq("client@example.com")))
                .willReturn(RUN_ID);

        // when / then
        mockMvc.perform(post("/proposals/{proposalId}/recommendations", PROPOSAL_ID)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("proposalPositionId", String.valueOf(PROPOSAL_POSITION_ID))
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect").value("/proposals/" + PROPOSAL_ID + "/runs/" + RUN_ID));

        then(recommendationRunService).should()
                .createOrReuse(eq(PROPOSAL_ID), eq(PROPOSAL_POSITION_ID), eq("client@example.com"));
    }

    @Test
    @DisplayName("추천 시작 AJAX 요청에서 포지션이 없으면 400과 JSON 에러를 반환한다")
    void runAjaxReturnsBadRequestWhenPositionMissing() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        // when / then — proposalPositionId 미전송
        mockMvc.perform(post("/proposals/{proposalId}/recommendations", PROPOSAL_ID)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("추천 시작 AJAX 요청 처리 중 예외가 발생하면 400과 JSON 에러를 반환한다")
    void runAjaxReturnsBadRequestOnException() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        given(recommendationRunService.createOrReuse(eq(PROPOSAL_ID), eq(PROPOSAL_POSITION_ID), eq("client@example.com")))
                .willThrow(new IllegalStateException("모집 중인 포지션만 추천을 실행할 수 있습니다."));

        // when / then
        mockMvc.perform(post("/proposals/{proposalId}/recommendations", PROPOSAL_ID)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("proposalPositionId", String.valueOf(PROPOSAL_POSITION_ID))
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── POST /proposal-positions/{proposalPositionId}/recommendations/more (AJAX) ──

    @Test
    @DisplayName("추가 추천 AJAX 요청이 성공하면 JSON으로 redirect URL을 반환한다")
    void moreAjaxReturnsRedirectUrl() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        given(recommendationRunService.createAdditional(eq(PROPOSAL_ID), eq(PROPOSAL_POSITION_ID), eq("client@example.com")))
                .willReturn(RUN_ID);

        // when / then
        mockMvc.perform(post("/proposal-positions/{proposalPositionId}/recommendations/more", PROPOSAL_POSITION_ID)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("proposalId", String.valueOf(PROPOSAL_ID))
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect").value("/proposals/" + PROPOSAL_ID + "/runs/" + RUN_ID));

        then(recommendationRunService).should()
                .createAdditional(eq(PROPOSAL_ID), eq(PROPOSAL_POSITION_ID), eq("client@example.com"));
    }

    @Test
    @DisplayName("추가 추천 AJAX 요청 처리 중 예외가 발생하면 400과 JSON 에러를 반환한다")
    void moreAjaxReturnsBadRequestOnException() throws Exception {
        // given
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        given(recommendationRunService.createAdditional(eq(PROPOSAL_ID), eq(PROPOSAL_POSITION_ID), eq("client@example.com")))
                .willThrow(new IllegalStateException("모집 중인 포지션만 추천을 실행할 수 있습니다."));

        // when / then
        mockMvc.perform(post("/proposal-positions/{proposalPositionId}/recommendations/more", PROPOSAL_POSITION_ID)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("proposalId", String.valueOf(PROPOSAL_ID))
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
