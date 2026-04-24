package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.dto.profile.ProfileAccessLevel;
import com.generic4.itda.dto.profile.ProfileCareerItemViewModel;
import com.generic4.itda.dto.profile.ProfileContextType;
import com.generic4.itda.dto.profile.ProfileFreelancerBodyViewModel;
import com.generic4.itda.dto.profile.ProfileMatchingContextViewModel;
import com.generic4.itda.dto.profile.ProfileProjectSummaryViewModel;
import com.generic4.itda.dto.profile.ProfileShellViewModel;
import com.generic4.itda.dto.profile.ProfileSkillItemViewModel;
import com.generic4.itda.dto.profile.ProfileSubjectType;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.MatchingProfileQueryService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(MatchingProfileController.class)
class MatchingProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchingProfileQueryService matchingProfileQueryService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("프리랜서 상대 프로필에서도 공통 프로젝트 요약이 렌더링된다")
    void renderCounterpartProfileWithSharedProjectSummary() throws Exception {
        given(matchingProfileQueryService.getCounterpartProfile(401L, "client@example.com"))
                .willReturn(createFreelancerShellView());

        mockMvc.perform(get("/matchings/401/counterpart-profile")
                        .param("backUrl", "/client/dashboard")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/shell"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/client/dashboard\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("요청 프로젝트 요약")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI 매칭 플랫폼")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("매칭 상태")));
    }

    @Test
    @DisplayName("없는 매칭 상대 프로필을 조회하면 이전 화면으로 돌아가며 오류 메시지를 남긴다")
    void redirectToBackUrlWhenProfileNotFound() throws Exception {
        given(matchingProfileQueryService.getCounterpartProfile(401L, "client@example.com"))
                .willThrow(new IllegalArgumentException("매칭 정보를 찾을 수 없습니다. id=401"));

        mockMvc.perform(get("/matchings/401/counterpart-profile")
                        .param("backUrl", "/matchings/401")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("errorMessage", "존재하지 않는 매칭입니다."));
    }

    @Test
    @DisplayName("외부 backUrl로 상대 프로필 접근이 실패하면 안전한 매칭 경로로 되돌린다")
    void redirectToSafeMatchingPathWhenBackUrlIsUnsafe() throws Exception {
        given(matchingProfileQueryService.getCounterpartProfile(401L, "client@example.com"))
                .willThrow(new AccessDeniedException("해당 매칭 정보에 접근할 수 없습니다."));

        mockMvc.perform(get("/matchings/401/counterpart-profile")
                        .param("backUrl", "https://example.com/outside")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matchings/401"))
                .andExpect(flash().attribute("errorMessage", "해당 매칭 정보에 접근할 수 없습니다."));
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

    private ProfileShellViewModel createFreelancerShellView() {
        return new ProfileShellViewModel(
                ProfileSubjectType.FREELANCER,
                ProfileContextType.MATCHING,
                ProfileAccessLevel.PREVIEW,
                "길*",
                "AI 매칭 플랫폼 · 플랫폼 백엔드 개발자",
                "제안됨",
                "/matchings/401",
                new ProfileFreelancerBodyViewModel(
                        "길*",
                        "프리랜서 프로필",
                        "커머스와 금융 도메인 백엔드 개발 경험이 있습니다.",
                        5,
                        "상주, 원격 모두 가능",
                        "https://portfolio.example.com",
                        List.of(
                                new ProfileSkillItemViewModel("Java", "고급", "ADVANCED"),
                                new ProfileSkillItemViewModel("Spring", "중급", "INTERMEDIATE")
                        ),
                        List.of(
                                new ProfileCareerItemViewModel(
                                        "네오핀",
                                        "백엔드 개발자",
                                        "정규직",
                                        "2021-01 ~ 2023-06",
                                        "결제 API와 정산 배치를 개발했습니다.",
                                        List.of("Java", "Spring Boot", "MySQL")
                                )
                        )
                ),
                null,
                new ProfileProjectSummaryViewModel(
                        200L,
                        "AI 매칭 플랫폼",
                        "AI 추천 기반 외주 매칭 프로젝트",
                        "플랫폼 백엔드 개발자",
                        "원격",
                        "3,000,000원 ~ 5,000,000원",
                        "4주",
                        List.of("Java"),
                        List.of("Docker")
                ),
                new ProfileMatchingContextViewModel(
                        401L,
                        "CLIENT",
                        "PROPOSED",
                        false,
                        "제안됨",
                        "프리랜서가 요청을 확인하고 응답하면 다음 단계로 넘어갈 수 있습니다.",
                        "/matchings/401",
                        "/proposals/200",
                        "/matchings/401/accept",
                        "/matchings/401/reject"
                ),
                null
        );
    }
}
