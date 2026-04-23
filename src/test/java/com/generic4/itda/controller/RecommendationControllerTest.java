package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.dto.recommend.RecommendationCandidateItem;
import com.generic4.itda.dto.recommend.RecommendationResumeCareerItem;
import com.generic4.itda.dto.recommend.RecommendationResumeDetailViewModel;
import com.generic4.itda.dto.recommend.RecommendationResumeSkillItem;
import com.generic4.itda.dto.recommend.RecommendationResultsViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.recommend.RecommendationEntryService;
import com.generic4.itda.service.recommend.RecommendationRunQueryService;
import com.generic4.itda.service.recommend.RecommendationRunService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    private RecommendationEntryService recommendationEntryService;

    @MockitoBean
    private RecommendationRunService recommendationRunService;

    @MockitoBean
    private RecommendationRunQueryService recommendationRunQueryService;

    @MockitoBean
    private MemberRepository memberRepository;

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
                List.of(new RecommendationResumeSkillItem("Java", "고급")),
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
                .andExpect(model().attributeExists("view"));

        then(recommendationRunQueryService).should()
                .getRecommendationCandidateResume(eq(PROPOSAL_ID), eq(RESULT_ID), eq("client@example.com"));
    }
}
