package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.dto.recommend.RecommendationEntryPositionItem;
import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.exception.ProposalNotFoundException;
import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;
import com.generic4.itda.dto.recommend.RecommendationEntryViewModel;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.PositionResolver;
import com.generic4.itda.service.ProposalAiInterviewService;
import com.generic4.itda.service.ProposalService;
import com.generic4.itda.service.recommend.RecommendationEntryService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ProposalAiInterviewService proposalAiInterviewService;

    @MockitoBean
    private PositionResolver positionResolver;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private MatchingRepository matchingRepository;

    @MockitoBean
    private RecommendationEntryService recommendationEntryService;

    @Test
    @DisplayName("인증된 사용자가 새 제안서 작성 화면을 조회하면 편집기를 렌더링한다")
    void renderNewProposalForm() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );
        given(positionResolver.findAllowedPositions())
                .willReturn(List.of(Position.create("백엔드 개발자")));

        mockMvc.perform(get("/proposals/new")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalForm"))
                .andExpect(model().attributeExists("proposalForm", "positionOptions", "workTypes", "aiInterviewMessages"))
                .andExpect(model().attribute("isNew", true))
                .andExpect(model().attribute("aiBriefRequested", false));
    }

    @Test
    @DisplayName("임시저장 요청이 유효하면 WRITING 저장 후 수정 화면으로 이동한다")
    void saveDraftAndRedirectToEdit() throws Exception {
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(1L);
        given(positionResolver.findAllowedPositions()).willReturn(List.of());
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
    @DisplayName("AI 브리프 요청과 함께 임시저장하면 수정 화면에서 자동 생성을 요청한다")
    void saveDraftAndRedirectToEditWithAiBriefRequested() throws Exception {
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(1L);
        given(positionResolver.findAllowedPositions()).willReturn(List.of());
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
                        .param("aiBriefRequested", "true")
                        .param("title", "AI 브리프 작성 중")
                        .param("rawInputText", "- 온라인 쇼핑몰 앱을 만들고 싶습니다."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/1/edit?aiBriefRequested=true"));

        then(proposalService).should().saveDraft(anyString(), any(ProposalForm.class));
    }

    @Test
    @DisplayName("제안서 등록 요청이 유효하면 제안서 상세 추천 모달로 이동한다")
    void registerAndRedirectToRecommendationEntry() throws Exception {
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(3L);
        given(positionResolver.findAllowedPositions())
                .willReturn(List.of(Position.create("백엔드 개발자")));
        // register 검증 로직에서 총 예산 계산이 null 이면 실패로 처리되므로, 성공 흐름을 위해 stub 한다.
        given(proposalService.calculateTotalBudgetMin(any(ProposalForm.class))).willReturn(3_000_000L);
        given(proposalService.calculateTotalBudgetMax(any(ProposalForm.class))).willReturn(4_000_000L);
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
                .andExpect(redirectedUrl("/proposals/3?openRecommendModal=true"));

        then(proposalService).should().register(anyString(), any(ProposalForm.class));
    }

    @Test
    @DisplayName("등록 조건을 만족하지 못하면 화면에 머무르며 이유를 보여준다")
    void stayOnFormWhenRegisterValidationFails() throws Exception {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );
        given(positionResolver.findAllowedPositions())
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
                        .param("rawInputText", "")
                        .param("aiBriefRequested", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalForm"))
                .andExpect(model().attributeHasErrors("proposalForm"))
                .andExpect(model().attributeExists("aiInterviewMessages"))
                .andExpect(model().attribute("aiBriefRequested", true));

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
        given(positionResolver.findAllowedPositions())
                .willReturn(List.of(Position.create("백엔드 개발자")));
        given(proposalService.findOwnedProposal(1L, "client@example.com")).willReturn(proposal);
        given(proposalService.findOwnedProposalForm(1L, "client@example.com"))
                .willReturn(ProposalForm.from(proposal));
        given(proposalAiInterviewService.findMessageResponses(1L, "client@example.com"))
                .willReturn(List.of());

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
                .andExpect(model().attributeExists("proposalForm", "aiInterviewMessages"))
                .andExpect(model().attribute("isNew", false))
                .andExpect(model().attribute("aiBriefRequested", false));
    }

    @Test
    @DisplayName("AI 브리프 요청 파라미터가 있으면 수정 화면 모델에 전달한다")
    void renderEditFormWithAiBriefRequested() throws Exception {
        Proposal proposal = Proposal.create(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678"),
                "AI 브리프 작성 중",
                "- 온라인 쇼핑몰 앱을 만들고 싶습니다.",
                "",
                null,
                null,
                null
        );
        given(positionResolver.findAllowedPositions())
                .willReturn(List.of(Position.create("백엔드 개발자")));
        given(proposalService.findOwnedProposal(1L, "client@example.com")).willReturn(proposal);
        given(proposalService.findOwnedProposalForm(1L, "client@example.com"))
                .willReturn(ProposalForm.from(proposal));
        given(proposalAiInterviewService.findMessageResponses(1L, "client@example.com"))
                .willReturn(List.of());

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(get("/proposals/1/edit")
                        .param("aiBriefRequested", "true")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalForm"))
                .andExpect(model().attributeExists("proposalForm", "aiInterviewMessages"))
                .andExpect(model().attribute("proposalId", 1L))
                .andExpect(model().attribute("isNew", false))
                .andExpect(model().attribute("aiBriefRequested", true));
    }

    @Test
    @DisplayName("수정 화면에서 AI 브리프 요청과 함께 임시저장하면 자동 생성 요청 파라미터를 유지한다")
    void updateDraftAndRedirectToEditWithAiBriefRequested() throws Exception {
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(1L);
        given(positionResolver.findAllowedPositions()).willReturn(List.of());
        given(proposalService.saveDraft(any(Long.class), anyString(), any(ProposalForm.class))).willReturn(proposal);

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/edit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .param("submitAction", "save")
                        .param("aiBriefRequested", "true")
                        .param("title", "AI 브리프 작성 중")
                        .param("rawInputText", "- 온라인 쇼핑몰 앱을 만들고 싶습니다."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/1/edit?aiBriefRequested=true"));

        then(proposalService).should().saveDraft(any(Long.class), anyString(), any(ProposalForm.class));
    }

    @Test
    @DisplayName("수정 요청이 유효하면 saveDraft 후 edit 화면으로 이동한다")
    void updateDraftAndRedirectToEdit() throws Exception {
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(1L);
        given(positionResolver.findAllowedPositions()).willReturn(List.of());
        given(proposalService.saveDraft(any(Long.class), anyString(), any(ProposalForm.class))).willReturn(proposal);

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/edit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .param("submitAction", "save")
                        .param("title", "수정된 제목")
                        .param("rawInputText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/1/edit"));

        then(proposalService).should().saveDraft(any(Long.class), anyString(), any(ProposalForm.class));
    }

    @Test
    @DisplayName("수정 요청에서 register 액션이 유효하면 제안서 상세 추천 모달로 이동한다")
    void registerAfterUpdateAndRedirectToRecommendationEntry() throws Exception {
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(1L);
        given(positionResolver.findAllowedPositions())
                .willReturn(List.of(Position.create("백엔드 개발자")));
        given(proposalService.calculateTotalBudgetMin(any(ProposalForm.class))).willReturn(3_000_000L);
        given(proposalService.calculateTotalBudgetMax(any(ProposalForm.class))).willReturn(4_000_000L);
        given(proposalService.register(any(Long.class), anyString(), any(ProposalForm.class))).willReturn(proposal);

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/edit")
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
                .andExpect(redirectedUrl("/proposals/1?openRecommendModal=true"));

        then(proposalService).should().register(any(Long.class), anyString(), any(ProposalForm.class));
    }

    @Test
    @DisplayName("수정 요청에서 register 조건을 만족하지 못하면 편집기에 머무르며 이유를 보여준다")
    void stayOnFormWhenUpdateRegisterValidationFails() throws Exception {
        given(positionResolver.findAllowedPositions())
                .willReturn(List.of(Position.create("백엔드 개발자")));

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/edit")
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

        then(proposalService).should(never()).register(any(Long.class), anyString(), any(ProposalForm.class));
    }

    @Test
    @DisplayName("수정 요청 시 제안서가 없으면 대시보드로 리다이렉트한다")
    void redirectDashboardWhenUpdateNotFound() throws Exception {
        willThrow(new ProposalNotFoundException("제안서를 찾을 수 없습니다. id=999"))
                .given(proposalService).saveDraft(eq(999L), eq("client@example.com"), any(ProposalForm.class));

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/999/edit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .param("submitAction", "save")
                        .param("title", "수정된 제목")
                        .param("rawInputText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("수정 요청 시 타인 제안서이면 대시보드로 리다이렉트한다")
    void redirectDashboardWhenUpdateAccessDenied() throws Exception {
        willThrow(new AccessDeniedException("본인 제안서만 조회하거나 수정할 수 있습니다."))
                .given(proposalService).saveDraft(eq(1L), eq("client@example.com"), any(ProposalForm.class));

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/edit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .param("submitAction", "save")
                        .param("title", "수정된 제목")
                        .param("rawInputText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("수정 요청 처리 중 예외가 발생하면 편집기에 머무르며 이유를 보여준다")
    void stayOnFormWhenUpdateFailsWithIllegalState() throws Exception {
        given(positionResolver.findAllowedPositions())
                .willReturn(List.of(Position.create("백엔드 개발자")));
        willThrow(new IllegalStateException("저장 중 오류가 발생했습니다."))
                .given(proposalService).saveDraft(eq(1L), eq("client@example.com"), any(ProposalForm.class));

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/edit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf())
                        .param("submitAction", "save")
                        .param("title", "수정된 제목")
                        .param("rawInputText", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalForm"))
                .andExpect(model().attributeHasErrors("proposalForm"));
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
    @DisplayName("COMPLETE 제안서는 GET /edit 에서 수정 불가 메시지와 함께 대시보드로 이동한다")
    void redirectDashboardWhenProposalIsComplete() throws Exception {
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
        proposal.complete();

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
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attributeExists("errorMessage"));
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
    @DisplayName("수정 시작 POST에서 새 draft로 이동하는 경우 noticeMessage 를 남긴다")
    void keepNoticeMessageWhenRedirectedToNewDraftAfterCreateEditDraft() throws Exception {
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
                .andExpect(redirectedUrl("/proposals/7/edit"))
                .andExpect(flash().attributeExists("noticeMessage"));
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

    @Test
    @DisplayName("없는 제안서로 edit 진입하면 대시보드로 돌아가고 오류 메시지를 남긴다")
    void redirectDashboardWhenProposalDoesNotExist() throws Exception {
        given(proposalService.findOwnedProposal(999L, "client@example.com"))
                .willThrow(new ProposalNotFoundException("제안서를 찾을 수 없습니다. id=999"));

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(get("/proposals/999/edit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"));
    }

    @Test
    @DisplayName("없는 제안서 상세 조회 시 대시보드로 리다이렉트한다")
    void redirectDashboardWhenDetailNotFound() throws Exception {
        given(proposalService.findOwnedProposal(999L, "client@example.com"))
                .willThrow(new ProposalNotFoundException("제안서를 찾을 수 없습니다. id=999"));

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(get("/proposals/999")
                        .header("Referer", "/client/dashboard")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("존재하지 않는 제안서 조회 시 홈으로 리다이렉트된다")
    void redirectHomeWhenDetailNotFoundWithExternalReferer() throws Exception {
        given(proposalService.findOwnedProposal(999L, "client@example.com"))
                .willThrow(new ProposalNotFoundException("제안서를 찾을 수 없습니다. id=999"));

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "client", "010-1234-5678")
        );

        mockMvc.perform(get("/proposals/999")
                        .header("Referer", "https://example.com/somewhere")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("제안서 상세 조회 권한이 없으면 홈으로 리다이렉트하고 에러 메시지를 남긴다")
    void redirectDashboardWhenDetailAccessDenied() throws Exception {
        given(proposalService.findOwnedProposal(1L, "client@example.com"))
                .willThrow(new AccessDeniedException("본인 제안서만 조회할 수 있습니다."));

        given(proposalService.findProposalForFreelancer(1L, "client@example.com"))
                .willThrow(new AccessDeniedException("매칭 이력이 없는 제안서입니다."));

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(get("/proposals/1")
                        .header("Referer", "/client/dashboard")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("프리랜서가 선택된 포지션 컨텍스트로 제안서 상세를 조회하면 해당 포지션을 모델에 담는다")
    void renderProposalDetailForFreelancerWithSelectedPositionContext() throws Exception {
        var freelancer = createMember("freelancer@example.com", "hashed-password", "프리랜서", "010-3333-4444");

        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        ProposalPosition backendPosition = org.mockito.Mockito.mock(ProposalPosition.class);
        ProposalPosition aiPosition = org.mockito.Mockito.mock(ProposalPosition.class);
        Matching backendMatching = org.mockito.Mockito.mock(Matching.class);
        Position backendCategory = Position.create("백엔드 개발자");
        Position aiCategory = Position.create("AI 엔지니어");

        given(proposal.getPositions()).willReturn(List.of(backendPosition, aiPosition));
        given(proposal.getId()).willReturn(1L);
        given(proposal.getTitle()).willReturn("핀테크 앱 고도화 프로젝트");
        given(proposal.getDescription()).willReturn("설명");
        given(proposal.getStatus()).willReturn(ProposalStatus.MATCHING);
        given(proposal.getExpectedPeriod()).willReturn(8L);
        given(proposal.getTotalBudgetMin()).willReturn(3_000_000L);
        given(proposal.getTotalBudgetMax()).willReturn(4_000_000L);
        given(proposal.getModifiedAt()).willReturn(LocalDateTime.of(2026, 4, 22, 12, 0));

        given(backendPosition.getId()).willReturn(10L);
        given(backendPosition.getTitle()).willReturn("API 백엔드 개발자");
        given(backendPosition.getPosition()).willReturn(backendCategory);
        given(backendPosition.getStatus()).willReturn(ProposalPositionStatus.OPEN);
        given(backendPosition.getHeadCount()).willReturn(1L);
        given(backendPosition.getUnitBudgetMin()).willReturn(3_000_000L);
        given(backendPosition.getUnitBudgetMax()).willReturn(4_000_000L);
        given(backendPosition.getExpectedPeriod()).willReturn(4L);
        given(backendPosition.getSkills()).willReturn(List.of());
        given(backendPosition.getWorkType()).willReturn(ProposalWorkType.REMOTE);
        given(backendPosition.getWorkPlace()).willReturn(null);
        given(backendPosition.getCareerMinYears()).willReturn(null);
        given(backendPosition.getCareerMaxYears()).willReturn(null);

        given(aiPosition.getId()).willReturn(20L);
        given(aiPosition.getTitle()).willReturn("모델/프롬프트 AI 엔지니어");
        given(aiPosition.getPosition()).willReturn(aiCategory);
        given(aiPosition.getStatus()).willReturn(ProposalPositionStatus.OPEN);
        given(aiPosition.getHeadCount()).willReturn(1L);
        given(aiPosition.getUnitBudgetMin()).willReturn(4_000_000L);
        given(aiPosition.getUnitBudgetMax()).willReturn(5_000_000L);
        given(aiPosition.getExpectedPeriod()).willReturn(6L);
        given(aiPosition.getSkills()).willReturn(List.of());
        given(aiPosition.getWorkType()).willReturn(ProposalWorkType.REMOTE);
        given(aiPosition.getWorkPlace()).willReturn(null);
        given(aiPosition.getCareerMinYears()).willReturn(null);
        given(aiPosition.getCareerMaxYears()).willReturn(null);

        given(backendMatching.getProposalPosition()).willReturn(backendPosition);
        given(backendMatching.getStatus()).willReturn(com.generic4.itda.domain.matching.constant.MatchingStatus.PROPOSED);
        given(backendMatching.getId()).willReturn(101L);

        given(proposalService.findOwnedProposal(1L, "freelancer@example.com"))
                .willThrow(new AccessDeniedException("본인 제안서만 조회할 수 있습니다."));
        given(proposalService.findProposalForFreelancer(1L, "freelancer@example.com"))
                .willReturn(proposal);
        given(matchingRepository.findByProposalPosition_Proposal_IdAndFreelancerMember_Email_Value(
                1L, "freelancer@example.com"))
                .willReturn(List.of(backendMatching));

        ItDaPrincipal principal = ItDaPrincipal.from(freelancer);

        mockMvc.perform(get("/proposals/1")
                        .param("proposalPositionId", "10")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalDetail"))
                .andExpect(model().attribute("viewerRole", "FREELANCER"))
                .andExpect(model().attribute("selectedProposalPositionId", 10L))
                .andExpect(model().attributeExists(
                        "proposal",
                        "selectedProposalPosition",
                        "matchingByPositionId",
                        "proposedPositions"
                ));
    }

    @Test
    @DisplayName("WRITING 상태 제안서 삭제 요청 시 대시보드로 리다이렉트한다")
    void deleteWritingProposalAndRedirectToDashboard() throws Exception {
        willDoNothing().given(proposalService).delete(1L, "client@example.com");

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/delete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"));

        then(proposalService).should().delete(1L, "client@example.com");
    }

    @Test
    @DisplayName("없는 제안서 삭제 시도 시 에러 메시지와 함께 대시보드로 리다이렉트한다")
    void redirectDashboardWithErrorWhenDeleteNotFound() throws Exception {
        willThrow(new ProposalNotFoundException("제안서를 찾을 수 없습니다. id=999"))
                .given(proposalService).delete(999L, "client@example.com");

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/999/delete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attributeExists("errorMessage"));

        then(proposalService).should().delete(999L, "client@example.com");
    }

    @Test
    @DisplayName("타인의 제안서 삭제 시도 시 에러 메시지와 함께 대시보드로 리다이렉트한다")
    void redirectDashboardWithErrorWhenDeleteAccessDenied() throws Exception {
        willThrow(new AccessDeniedException("본인 제안서만 삭제할 수 있습니다."))
                .given(proposalService).delete(1L, "client@example.com");

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/delete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"));

        then(proposalService).should().delete(1L, "client@example.com");
    }

    @Test
    @DisplayName("삭제 불가 상태의 제안서 삭제 시도 시 에러 메시지와 함께 대시보드로 리다이렉트한다")
    void redirectDashboardWithErrorWhenDeleteFails() throws Exception {
        willThrow(new IllegalStateException("작성 중인 제안서만 삭제할 수 있습니다."))
                .given(proposalService).delete(1L, "client@example.com");

        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678")
        );

        mockMvc.perform(post("/proposals/1/delete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"));

        then(proposalService).should().delete(1L, "client@example.com");
    }

    @Test
    @DisplayName("MATCHING 상태 제안서 상세 조회 시 recommendEntry 모델에 포함되고 모달 DOM이 렌더된다")
    void detailForMatchingProposalIncludesRecommendEntry() throws Exception {
        var client = createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678");
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(1L);
        given(proposal.getTitle()).willReturn("핀테크 앱 프로젝트");
        given(proposal.getDescription()).willReturn("설명");
        given(proposal.getStatus()).willReturn(ProposalStatus.MATCHING);
        given(proposal.getExpectedPeriod()).willReturn(8L);
        given(proposal.getTotalBudgetMin()).willReturn(3_000_000L);
        given(proposal.getTotalBudgetMax()).willReturn(4_000_000L);
        given(proposal.getModifiedAt()).willReturn(java.time.LocalDateTime.of(2026, 4, 23, 12, 0));
        given(proposal.getPositions()).willReturn(List.of());

        RecommendationEntryPositionItem positionItem = new RecommendationEntryPositionItem(
                10L, "백엔드 개발자", "백엔드", 2L, "3,000,000 ~ 5,000,000", 8L, List.of(),
                "OPEN", "모집 중"
        );
        RecommendationEntryViewModel recommendEntry = new RecommendationEntryViewModel(
                1L, "핀테크 앱 프로젝트", "MATCHING", true,
                "추천을 실행할 수 있습니다.", 10L, List.of(positionItem)
        );

        given(proposalService.findOwnedProposal(1L, "client@example.com")).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(1L, "client@example.com"))
                .willReturn(List.of());
        given(recommendationEntryService.getEntry(1L, "client@example.com")).willReturn(recommendEntry);

        ItDaPrincipal principal = ItDaPrincipal.from(client);

        mockMvc.perform(get("/proposals/1")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalDetail"))
                .andExpect(model().attribute("viewerRole", "CLIENT"))
                .andExpect(model().attributeExists("recommendEntry"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"recommendModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"recommendModalBackdrop\"")));
    }

    @Test
    @DisplayName("MATCHING 제안서에서 getEntry 예외 발생 시에도 페이지는 200으로 렌더되고 모달은 표시되지 않는다")
    void detailPageRendersEvenWhenGetEntryFails() throws Exception {
        var client = createMember("client@example.com", "hashed-password", "클라이언트", "010-1234-5678");
        Proposal proposal = org.mockito.Mockito.mock(Proposal.class);
        given(proposal.getId()).willReturn(1L);
        given(proposal.getTitle()).willReturn("핀테크 앱 프로젝트");
        given(proposal.getDescription()).willReturn("설명");
        given(proposal.getStatus()).willReturn(ProposalStatus.MATCHING);
        given(proposal.getExpectedPeriod()).willReturn(8L);
        given(proposal.getTotalBudgetMin()).willReturn(3_000_000L);
        given(proposal.getTotalBudgetMax()).willReturn(4_000_000L);
        given(proposal.getModifiedAt()).willReturn(java.time.LocalDateTime.of(2026, 4, 23, 12, 0));
        given(proposal.getPositions()).willReturn(List.of());

        given(proposalService.findOwnedProposal(1L, "client@example.com")).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(1L, "client@example.com"))
                .willReturn(List.of());
        given(recommendationEntryService.getEntry(1L, "client@example.com"))
                .willThrow(new IllegalArgumentException("존재하지 않는 제안서입니다."));

        ItDaPrincipal principal = ItDaPrincipal.from(client);

        mockMvc.perform(get("/proposals/1")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(view().name("client/proposalDetail"))
                .andExpect(model().attribute("viewerRole", "CLIENT"))
                .andExpect(model().attributeDoesNotExist("recommendEntry"));
    }
}
