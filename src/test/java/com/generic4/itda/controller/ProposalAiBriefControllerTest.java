package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.proposal.AiBriefPositionResult;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.proposal.AiBriefSkillResult;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.ProposalAiBriefService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(ProposalAiBriefController.class)
class ProposalAiBriefControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProposalAiBriefService proposalAiBriefService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("인증된 사용자가 AI 브리프 생성을 요청하면 JSON 결과를 반환한다")
    void generateAiBrief() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("user@example.com", "hashed-password", "사용자", "010-1234-5678")
        );
        AiBriefResult result = AiBriefResult.of(
                "AI가 만든 제목",
                "AI가 만든 설명",
                3_000_000L,
                5_000_000L,
                6L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "Node.js 백엔드 개발자",
                                ProposalWorkType.HYBRID,
                                1L,
                                3_000_000L,
                                4_000_000L,
                                6L,
                                3,
                                6,
                                "판교",
                                List.of(
                                        AiBriefSkillResult.of("Java", ProposalPositionSkillImportance.ESSENTIAL),
                                        AiBriefSkillResult.of("Spring Boot",
                                                ProposalPositionSkillImportance.PREFERENCE)
                                )
                        )
                )
        );
        given(proposalAiBriefService.generate(1L, "user@example.com")).willReturn(result);

        mockMvc.perform(post("/proposals/{proposalId}/ai-brief", 1L)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("AI가 만든 제목"))
                .andExpect(jsonPath("$.description").value("AI가 만든 설명"))
                .andExpect(jsonPath("$.positions[0].positionCategoryName").value("백엔드 개발자"))
                .andExpect(jsonPath("$.positions[0].title").value("Node.js 백엔드 개발자"))
                .andExpect(jsonPath("$.positions[0].workType").value("HYBRID"))
                .andExpect(jsonPath("$.positions[0].skills[0].skillName").value("Java"))
                .andExpect(jsonPath("$.positions[0].skills[0].importance").value("ESSENTIAL"));

        then(proposalAiBriefService).should().generate(1L, "user@example.com");
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 로그인 페이지로 리다이렉트된다")
    void redirectToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/proposals/{proposalId}/ai-brief", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
