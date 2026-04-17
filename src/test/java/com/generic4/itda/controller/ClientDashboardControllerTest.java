package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.dto.client.ClientDashboardProjectItem;
import com.generic4.itda.dto.client.ClientDashboardSummaryItem;
import com.generic4.itda.dto.client.ClientDashboardViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.ClientDashboardService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(ClientDashboardController.class)
class ClientDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientDashboardService clientDashboardService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("인증된 사용자가 클라이언트 대시보드를 조회하면 화면과 모델을 반환한다")
    void renderClientDashboard() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );
        ClientDashboardViewModel dashboard = new ClientDashboardViewModel(
                "matching",
                "진행 중 매칭",
                List.of(
                        new ClientDashboardSummaryItem("all", "전체 프로젝트", 6L, "전체", false),
                        new ClientDashboardSummaryItem("matching", "진행 중 매칭", 3L, "진행 중", true)
                ),
                List.of(
                        new ClientDashboardProjectItem(
                                1L,
                                "핀테크 앱 고도화 프로젝트",
                                "MATCHING",
                                "모집/추천 진행 중",
                                3,
                                "4,500,000원",
                                "2024.03.15",
                                "추천/매칭 진행 중"
                        )
                )
        );
        given(clientDashboardService.getDashboard("client@example.com", com.generic4.itda.dto.client.ClientDashboardFilter.MATCHING))
                .willReturn(dashboard);

        mockMvc.perform(get("/client/dashboard")
                        .param("filter", "matching")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("client/dashboard"))
                .andExpect(model().attribute("dashboard", dashboard))
                .andExpect(model().attribute("memberName", "클라이언트"))
                .andExpect(model().attribute("memberEmail", "client@example.com"));

        then(clientDashboardService).should()
                .getDashboard("client@example.com", com.generic4.itda.dto.client.ClientDashboardFilter.MATCHING);
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 로그인 페이지로 리다이렉트된다")
    void redirectToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/client/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
