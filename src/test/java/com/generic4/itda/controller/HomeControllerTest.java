package com.generic4.itda.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.domain.member.UserRole;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberRepository memberRepository;

    @Mock
    private ItDaPrincipal principal;

    @Test
    @DisplayName("비로그인 사용자는 홈 화면을 볼 수 있다")
    void renderHomePageForGuest() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("authenticated", false));
    }

    @Test
    @DisplayName("로그인 사용자는 홈 화면에서 기본 사용자 정보를 본다")
    void renderHomePageForAuthenticatedUser() throws Exception {
        given(principal.getName()).willReturn("홍길동");
        given(principal.getEmail()).willReturn("member@example.com");
        given(principal.getRole()).willReturn(UserRole.USER);

        mockMvc.perform(get("/")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("authenticated", true))
                .andExpect(model().attribute("memberName", "홍길동"))
                .andExpect(model().attribute("memberEmail", "member@example.com"))
                .andExpect(model().attribute("memberRole", "USER"));
    }
}
