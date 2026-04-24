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
    @DisplayName("프리랜서 상대 프로필에서도 returnTo 기반 back 링크와 공통 프로젝트 요약이 렌더링된다")
    void renderCounterpartProfileWithSharedProjectSummary() throws Exception {
        given(matchingProfileQueryService.getCounterpartProfile(401L, "client@example.com"))
                .willReturn(createFreelancerShellView(false));

        mockMvc.perform(get("/matchings/401/counterpart-profile")
                        .param("returnTo", "/client/dashboard")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/shell"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/client/dashboard\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("요청 프로젝트 요약")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI 매칭 플랫폼")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("매칭 상태")));
    }

    @Test
    @DisplayName("returnTo가 있으면 기존 backUrl보다 우선 적용한다")
    void renderCounterpartProfilePrefersReturnToOverBackUrl() throws Exception {
        given(matchingProfileQueryService.getCounterpartProfile(401L, "client@example.com"))
                .willReturn(createFreelancerShellView(false));

        mockMvc.perform(get("/matchings/401/counterpart-profile")
                        .param("returnTo", "/proposals/10/matchings")
                        .param("backUrl", "/matchings/401")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/shell"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/proposals/10/matchings\"")));
    }

    @Test
    @DisplayName("연락처가 공개된 매칭 프로필에서는 상대 연락처를 함께 렌더링한다")
    void renderCounterpartProfileWithContactDetailsWhenVisible() throws Exception {
        given(matchingProfileQueryService.getCounterpartProfile(401L, "client@example.com"))
                .willReturn(createFreelancerShellView(true));

        mockMvc.perform(get("/matchings/401/counterpart-profile")
                        .param("backUrl", "/matchings/401")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/shell"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("공개된 연락처")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("freelancer@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("010-0000-0002")));
    }

    @Test
    @DisplayName("없는 매칭 상대 프로필을 조회하면 이전 화면으로 돌아가며 오류 메시지를 남긴다")
    void redirectToBackUrlWhenProfileNotFound() throws Exception {
        given(matchingProfileQueryService.getCounterpartProfile(401L, "client@example.com"))
                .willThrow(new IllegalArgumentException("매칭 정보를 찾을 수 없습니다. id=401"));

        mockMvc.perform(get("/matchings/401/counterpart-profile")
                        .param("returnTo", "/proposals/10/matchings?status=PROPOSED")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/10/matchings?status=PROPOSED"))
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

    @Test
    @DisplayName("외부 returnTo는 무시하고 기존 내부 backUrl을 사용한다")
    void redirectToLegacyBackUrlWhenReturnToIsUnsafe() throws Exception {
        given(matchingProfileQueryService.getCounterpartProfile(401L, "client@example.com"))
                .willThrow(new AccessDeniedException("해당 매칭 정보에 접근할 수 없습니다."));

        mockMvc.perform(get("/matchings/401/counterpart-profile")
                        .param("returnTo", "https://example.com/outside")
                        .param("backUrl", "/matchings/401")
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

    private ProfileShellViewModel createFreelancerShellView(boolean contactVisible) {
        return new ProfileShellViewModel(
                ProfileSubjectType.FREELANCER,
                ProfileContextType.MATCHING,
                contactVisible ? ProfileAccessLevel.FULL : ProfileAccessLevel.PREVIEW,
                contactVisible ? "길동" : "길*",
                "AI 매칭 플랫폼 · 플랫폼 백엔드 개발자",
                contactVisible ? "수락됨" : "제안됨",
                "/matchings/401",
                new ProfileFreelancerBodyViewModel(
                        contactVisible ? "길동" : "길*",
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
                        contactVisible ? "ACCEPTED" : "PROPOSED",
                        contactVisible,
                        contactVisible ? "수락됨" : "제안됨",
                        contactVisible
                                ? "연락처가 공개되었습니다. 제안서를 다시 확인하고 협의를 이어가세요."
                                : "프리랜서가 요청을 확인하고 응답하면 다음 단계로 넘어갈 수 있습니다.",
                        "/matchings/401",
                        "/proposals/200",
                        "/matchings/401/accept",
                        "/matchings/401/reject",
                        contactVisible ? "freelancer@example.com" : null,
                        contactVisible ? "010-0000-0002" : null
                ),
                null
        );
    }
}
