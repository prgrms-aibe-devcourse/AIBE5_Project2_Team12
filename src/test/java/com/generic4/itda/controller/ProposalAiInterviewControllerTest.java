package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalAiInterviewMessage;
import com.generic4.itda.dto.proposal.AiInterviewMessageResponse;
import com.generic4.itda.dto.proposal.AiInterviewSendMessageResponse;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.ProposalAiInterviewService;
import com.generic4.itda.service.ProposalService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(ProposalAiInterviewController.class)
class ProposalAiInterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProposalAiInterviewService proposalAiInterviewService;

    @MockitoBean
    private ProposalService proposalService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("인증된 사용자가 AI 인터뷰 draft를 JSON으로 저장하면 204를 반환한다")
    void saveDraft() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("user@example.com", "hashed-password", "사용자", "010-1234-5678")
        );

        mockMvc.perform(patch("/proposals/{proposalId}/ai-interview/draft", 1L)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "온라인 쇼핑몰 구축",
                                  "description": "결제와 관리자 기능이 있는 쇼핑몰을 개발합니다.",
                                  "expectedPeriod": 12,
                                  "positions": [
                                    {
                                      "id": 1,
                                      "positionId": 3,
                                      "title": "백엔드 개발자",
                                      "workType": "REMOTE",
                                      "headCount": 2,
                                      "unitBudgetMin": 3000000,
                                      "unitBudgetMax": 5000000,
                                      "expectedPeriod": 8,
                                      "careerMinYears": 2,
                                      "careerMaxYears": 5,
                                      "workPlace": "",
                                      "status": "OPEN",
                                      "essentialSkillNames": ["Java", "Spring Boot"],
                                      "preferredSkillNames": ["AWS"]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isNoContent());

        then(proposalService).should().saveInterviewDraft(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("user@example.com"),
                org.mockito.ArgumentMatchers.argThat(request ->
                        "온라인 쇼핑몰 구축".equals(request.getTitle())
                                && "결제와 관리자 기능이 있는 쇼핑몰을 개발합니다.".equals(request.getDescription())
                                && Long.valueOf(12L).equals(request.getExpectedPeriod())
                                && request.getPositions().size() == 1
                                && Long.valueOf(3L).equals(request.getPositions().get(0).getPositionId())
                                && "백엔드 개발자".equals(request.getPositions().get(0).getTitle())
                )
        );
    }

    @Test
    @DisplayName("AI 인터뷰 draft 제목이 비어 있으면 400을 반환한다")
    void failWhenDraftTitleIsBlank() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("user@example.com", "hashed-password", "사용자", "010-1234-5678")
        );

        mockMvc.perform(patch("/proposals/{proposalId}/ai-interview/draft", 1L)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "설명",
                                  "expectedPeriod": 12,
                                  "positions": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        then(proposalService).should(never()).saveInterviewDraft(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 AI 인터뷰 draft 저장 시 로그인 페이지로 리다이렉트된다")
    void redirectToLoginWhenSaveDraftUnauthenticated() throws Exception {
        mockMvc.perform(patch("/proposals/{proposalId}/ai-interview/draft", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "온라인 쇼핑몰 구축",
                                  "description": "설명",
                                  "expectedPeriod": 12,
                                  "positions": []
                                }
                                """))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("인증된 사용자가 AI 인터뷰 메시지를 전송하면 갱신된 폼과 메시지 목록을 반환한다")
    void sendMessage() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("user@example.com", "hashed-password", "사용자", "010-1234-5678")
        );
        Proposal proposal = Proposal.create(
                createMember("user@example.com", "hashed-password", "사용자", "010-1234-5678"),
                "제안서 제목",
                "",
                "제안서 본문",
                null,
                null,
                null
        );
        ProposalAiInterviewMessage userMessage = ProposalAiInterviewMessage.createUserMessage(
                proposal,
                "온라인 쇼핑몰 웹사이트를 만들고 싶어요.",
                1
        );
        ProposalAiInterviewMessage assistantMessage = ProposalAiInterviewMessage.createAssistantMessage(
                proposal,
                "필요한 주요 기능을 알려주세요.",
                2
        );
        ProposalForm proposalForm = ProposalForm.from(proposal);
        proposalForm.setTitle("온라인 쇼핑몰 구축");

        AiInterviewSendMessageResponse response = AiInterviewSendMessageResponse.of(
                proposalForm,
                List.of(
                        AiInterviewMessageResponse.from(userMessage),
                        AiInterviewMessageResponse.from(assistantMessage)
                ),
                "필요한 주요 기능을 알려주세요."
        );

        given(proposalAiInterviewService.sendMessage(
                1L,
                "user@example.com",
                "온라인 쇼핑몰 웹사이트를 만들고 싶어요."
        )).willReturn(response);

        mockMvc.perform(post("/proposals/{proposalId}/ai-interview/messages", 1L)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "온라인 쇼핑몰 웹사이트를 만들고 싶어요."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposalForm.title").value("온라인 쇼핑몰 구축"))
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[0].content").value("온라인 쇼핑몰 웹사이트를 만들고 싶어요."))
                .andExpect(jsonPath("$.messages[0].sequence").value(1))
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.messages[1].content").value("필요한 주요 기능을 알려주세요."))
                .andExpect(jsonPath("$.messages[1].sequence").value(2))
                .andExpect(jsonPath("$.assistantMessage").value("필요한 주요 기능을 알려주세요."));

        then(proposalAiInterviewService).should().sendMessage(
                1L,
                "user@example.com",
                "온라인 쇼핑몰 웹사이트를 만들고 싶어요."
        );
    }

    @Test
    @DisplayName("AI 인터뷰 메시지가 비어 있으면 400을 반환한다")
    void failWhenMessageIsBlank() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("user@example.com", "hashed-password", "사용자", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/{proposalId}/ai-interview/messages", 1L)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        then(proposalAiInterviewService).should(never()).sendMessage(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 로그인 페이지로 리다이렉트된다")
    void redirectToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/proposals/{proposalId}/ai-interview/messages", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "온라인 쇼핑몰 웹사이트를 만들고 싶어요."
                                }
                                """))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}