package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.service.ProposalService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(ProposalController.class)
class ProposalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProposalService proposalService;

    @MockitoBean
    private PositionRepository positionRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName("인증된 사용자가 새 제안서 작성 화면을 조회하면 편집기를 렌더링한다")
    void renderNewProposalForm() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );
        given(positionRepository.findAll(any(Sort.class)))
                .willReturn(List.of(Position.create("백엔드 개발자")));

        mockMvc.perform(get("/proposals/new")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalForm"))
                .andExpect(model().attributeExists("proposalForm", "positionOptions", "workTypes"))
                .andExpect(model().attribute("isNew", true));
    }

    @Test
    @DisplayName("임시저장 요청이 유효하면 WRITING 저장 후 수정 화면으로 이동한다")
    void saveDraftAndRedirectToEdit() throws Exception {
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(1L);
        given(positionRepository.findAll(any(Sort.class))).willReturn(List.of());
        given(proposalService.saveDraft(anyString(), any(ProposalForm.class))).willReturn(proposal);

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/new")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .param("submitAction", "save")
                        .param("title", "새 프로젝트")
                        .param("rawInputText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/1/edit"));

        then(proposalService).should().saveDraft(anyString(), any(ProposalForm.class));
    }

    @Test
    @DisplayName("제안서 등록 요청이 유효하면 추천 진입 화면으로 이동한다")
    void registerAndRedirectToRecommendationEntry() throws Exception {
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(3L);
        given(positionRepository.findAll(any(Sort.class)))
                .willReturn(List.of(Position.create("백엔드 개발자")));
        given(proposalService.register(anyString(), any(ProposalForm.class))).willReturn(proposal);

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/new")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .param("submitAction", "register")
                        .param("title", "쇼핑몰 앱 개발")
                        .param("rawInputText", "")
                        .param("expectedPeriod", "8")
                        .param("positions[0].positionId", "1")
                        .param("positions[0].title", "Node.js 백엔드 개발자")
                        .param("positions[0].workType", "REMOTE")
                        .param("positions[0].headCount", "1")
                        .param("positions[0].unitBudgetMin", "3000000")
                        .param("positions[0].unitBudgetMax", "4000000")
                        .param("positions[0].expectedPeriod", "4")
                        .param("positions[0].essentialSkillNames[0]", "Node.js"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/3/recommendations"));

        then(proposalService).should().register(anyString(), any(ProposalForm.class));
    }

    @Test
    @DisplayName("등록 조건을 만족하지 못하면 화면에 머무르며 이유를 보여준다")
    void stayOnFormWhenRegisterValidationFails() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );
        given(positionRepository.findAll(any(Sort.class)))
                .willReturn(List.of(Position.create("백엔드 개발자")));

        mockMvc.perform(post("/proposals/new")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .param("submitAction", "register")
                        .param("title", "쇼핑몰 앱 개발")
                        .param("rawInputText", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalForm"))
                .andExpect(model().attributeHasErrors("proposalForm"));

        then(proposalService).should(never()).register(anyString(), any(ProposalForm.class));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 로그인 페이지로 이동한다")
    void redirectToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/proposals/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("작성 중인 제안서는 GET /edit 에서 바로 편집기를 렌더링한다")
    void renderEditFormWhenProposalIsWriting() throws Exception {
        Proposal proposal = Proposal.create(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678"),
                "기존 프로젝트",
                "",
                "설명",
                null,
                null,
                6L
        );
        given(positionRepository.findAll(any(Sort.class)))
                .willReturn(List.of(Position.create("백엔드 개발자")));
        given(proposalService.findOwnedProposal(1L, "client@example.com")).willReturn(proposal);

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(get("/proposals/1/edit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalForm"))
                .andExpect(model().attributeExists("proposalForm"))
                .andExpect(model().attribute("isNew", false));
    }

    @Test
    @DisplayName("MATCHING 제안서는 GET /edit 에서 수정 시작 안내 화면을 보여준다")
    void renderEditStartWhenMatchingProposalNeedsDraft() throws Exception {
        Proposal proposal = Proposal.create(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678"),
                "기존 프로젝트",
                "",
                "설명",
                3_000_000L,
                4_000_000L,
                8L
        );
        proposal.startMatching();

        given(proposalService.findOwnedProposal(1L, "client@example.com")).willReturn(proposal);

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(get("/proposals/1/edit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalEditStart"))
                .andExpect(model().attributeExists("proposal"))
                .andExpect(model().attribute("proposalId", 1L));

        then(proposalService).should().validateCanCreateEditDraft(1L, "client@example.com");
    }

    @Test
    @DisplayName("수정 시작 POST는 새 draft를 만든 뒤 그 draft의 edit 화면으로 이동한다")
    void redirectToClonedDraftAfterCreateEditDraft() throws Exception {
        Proposal copied = org.mockito.Mockito.mock(Proposal.class);
        given(copied.getId()).willReturn(7L);
        given(proposalService.createEditDraft(1L, "client@example.com")).willReturn(copied);

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/edit-draft")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/7/edit"));
    }

    @Test
    @DisplayName("진행 중 또는 완료된 매칭 이력이 있으면 수정 시작 화면 대신 대시보드로 이동한다")
    void redirectDashboardWhenEditBlocked() throws Exception {
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getStatus()).willReturn(ProposalStatus.MATCHING);
        given(proposalService.findOwnedProposal(1L, "client@example.com")).willReturn(proposal);
        willThrow(new IllegalStateException("진행 중이거나 완료된 매칭이 있는 제안서는 수정할 수 없습니다."))
                .given(proposalService)
                .validateCanCreateEditDraft(1L, "client@example.com");

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(get("/proposals/1/edit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"));
    }
}
