package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.matching.ClientMatchingListItemViewModel;
import com.generic4.itda.dto.matching.ClientMatchingListViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.ProposalService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ControllerTest(MatchingEntryController.class)
class MatchingEntryControllerTest {

    private static final Long PROPOSAL_ID = 10L;
    private static final String CLIENT_EMAIL = "client@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProposalService proposalService;

    @MockitoBean
    private MatchingRepository matchingRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    // ── 매칭 목록 진입 ───────────────────────────────────────────

    @Test
    @DisplayName("매칭 목록 페이지 렌더링 — 포지션 필터 및 전체 목록 포함")
    void list_rendersAllMatchingsWithPositionFilters() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("matching/list"))
                .andReturn();

        ClientMatchingListViewModel view = (ClientMatchingListViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(view.proposalId()).isEqualTo(PROPOSAL_ID);
        assertThat(view.positionFilters()).hasSize(2);
        assertThat(view.items()).hasSize(3);
        assertThat(view.selectedPositionId()).isNull();
        assertThat(view.selectedStatus()).isNull();
        assertThat(result.getModelAndView().getModel().get("backUrl")).isEqualTo("/proposals/10");
        assertThat(result.getModelAndView().getModel().get("detailBackUrl"))
                .isEqualTo("/proposals/10/matchings?backUrl=/proposals/10");
        assertThat(result.getResponse().getContentAsString()).contains("href=\"/proposals/10\"");
        assertThat(result.getResponse().getContentAsString())
                .contains("counterpart-profile?returnTo=/proposals/10/matchings?backUrl%3D/proposals/10");
    }

    @Test
    @DisplayName("매칭 목록 페이지는 전달된 backUrl을 사용하고 상세 복귀 경로에도 유지한다")
    void list_preservesProvidedBackUrl() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .param("backUrl", "/client/dashboard")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("matching/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "counterpart-profile?returnTo=/proposals/10/matchings?backUrl%3D/client/dashboard")))
                .andReturn();

        assertThat(result.getModelAndView().getModel().get("backUrl")).isEqualTo("/client/dashboard");
        assertThat(result.getModelAndView().getModel().get("detailBackUrl"))
                .isEqualTo("/proposals/10/matchings?backUrl=/client/dashboard");
    }

    @Test
    @DisplayName("외부 backUrl은 무시하고 제안서 상세로 복귀시킨다")
    void list_ignoresExternalBackUrl() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .param("backUrl", "https://example.com/outside")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getModelAndView().getModel().get("backUrl")).isEqualTo("/proposals/10");
        assertThat(result.getModelAndView().getModel().get("detailBackUrl"))
                .isEqualTo("/proposals/10/matchings?backUrl=/proposals/10");
    }

    @Test
    @DisplayName("포지션 필터 적용 — 해당 포지션 매칭만 반환")
    void list_positionFilter_returnsOnlyMatchingsForPosition() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .param("positionId", "1")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        ClientMatchingListViewModel view = (ClientMatchingListViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(view.selectedPositionId()).isEqualTo(1L);
        assertThat(view.items()).hasSize(2); // pos1에 매칭 2개
        assertThat(view.items()).allMatch(item -> item.proposalPositionId().equals(1L));
    }

    @Test
    @DisplayName("상태 필터 적용 — 해당 상태 매칭만 반환")
    void list_statusFilter_returnsOnlyMatchingsForStatus() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .param("status", "PROPOSED")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        ClientMatchingListViewModel view = (ClientMatchingListViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(view.selectedStatus()).isEqualTo("PROPOSED");
        assertThat(view.items()).allMatch(item -> item.status() == MatchingStatus.PROPOSED);
    }

    @Test
    @DisplayName("목록 정렬 — PROPOSED > IN_PROGRESS > ACCEPTED > REJECTED > COMPLETED > CANCELED 순")
    void list_sortOrder_followsStatusPriority() throws Exception {
        Proposal proposal = createProposalWithSinglePosition();
        List<Matching> matchings = List.of(
                createMatching(103L, MatchingStatus.REJECTED, "프리C", proposal, LocalDateTime.of(2026, 4, 20, 0, 0)),
                createMatching(101L, MatchingStatus.PROPOSED, "프리A", proposal, LocalDateTime.of(2026, 4, 22, 0, 0)),
                createMatching(102L, MatchingStatus.IN_PROGRESS, "프리B", proposal, LocalDateTime.of(2026, 4, 21, 0, 0))
        );

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        ClientMatchingListViewModel view = (ClientMatchingListViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(view.items().get(0).status()).isEqualTo(MatchingStatus.PROPOSED);
        assertThat(view.items().get(1).status()).isEqualTo(MatchingStatus.IN_PROGRESS);
        assertThat(view.items().get(2).status()).isEqualTo(MatchingStatus.REJECTED);
    }

    @Test
    @DisplayName("포지션+상태 복합 필터 — 해당 포지션의 해당 상태 매칭만 반환")
    void list_combinedFilter_returnsMatchingBothConditions() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .param("positionId", "1")
                        .param("status", "PROPOSED")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        ClientMatchingListViewModel view = (ClientMatchingListViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(view.selectedPositionId()).isEqualTo(1L);
        assertThat(view.selectedStatus()).isEqualTo("PROPOSED");
        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).proposalPositionId()).isEqualTo(1L);
        assertThat(view.items().get(0).status()).isEqualTo(MatchingStatus.PROPOSED);
    }

    @Test
    @DisplayName("포지션 필터 카운트 — 각 포지션별 매칭 수 정확히 반영")
    void list_positionFilterCounts_areCorrect() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal); // pos1:2개, pos2:1개

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        ClientMatchingListViewModel view = (ClientMatchingListViewModel)
                result.getModelAndView().getModel().get("view");

        var pos1Filter = view.positionFilters().stream().filter(f -> f.proposalPositionId().equals(1L)).findFirst().orElseThrow();
        var pos2Filter = view.positionFilters().stream().filter(f -> f.proposalPositionId().equals(2L)).findFirst().orElseThrow();

        assertThat(pos1Filter.count()).isEqualTo(2L);
        assertThat(pos2Filter.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("매칭 없는 경우 — 빈 목록 반환")
    void list_noMatchings_returnsEmptyItems() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(List.of());

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        ClientMatchingListViewModel view = (ClientMatchingListViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(view.items()).isEmpty();
        assertThat(view.positionFilters()).allMatch(f -> f.count() == 0L);
    }

    @Test
    @DisplayName("잘못된 status 파라미터 — 무시하고 전체 목록 반환")
    void list_invalidStatus_ignoresFilterAndReturnsAll() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .param("status", "INVALID_STATUS_XYZ")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        ClientMatchingListViewModel view = (ClientMatchingListViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(view.selectedStatus()).isNull();
        assertThat(view.items()).hasSize(3);
    }

    @Test
    @DisplayName("프리랜서 이름 — 닉네임이 있으면 닉네임, 없으면 name 사용")
    void list_freelancerName_usesNicknameIfPresent() throws Exception {
        Proposal proposal = createProposalWithSinglePosition();
        Member client = proposal.getMember();
        ProposalPosition pos = proposal.getPositions().iterator().next();

        Member withNickname = createMember("fn1@example.com", "pw", "실명A", "닉네임A", "010-1111-0001");
        Member withoutNickname = createMember("fn2@example.com", "pw", "실명B", "010-1111-0002");

        Matching m1 = Matching.create(null, pos, client, withNickname);
        Matching m2 = Matching.create(null, pos, client, withoutNickname);
        ReflectionTestUtils.setField(m1, "id", 31L);
        ReflectionTestUtils.setField(m1, "status", MatchingStatus.PROPOSED);
        ReflectionTestUtils.setField(m2, "id", 32L);
        ReflectionTestUtils.setField(m2, "status", MatchingStatus.PROPOSED);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(List.of(m1, m2));

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        ClientMatchingListViewModel view = (ClientMatchingListViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(view.items()).anyMatch(item -> item.freelancerName().equals("닉네임A"));
        assertThat(view.items()).anyMatch(item -> item.freelancerName().equals("실명B"));
    }

    // ── AJAX 필터 엔드포인트 ─────────────────────────────────────

    @Test
    @DisplayName("AJAX 아이템 요청 — 상태 필터 적용 후 fragment 반환")
    void listItems_ajaxStatusFilter_returnsFragment() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        mockMvc.perform(get("/proposals/{id}/matchings/items", PROPOSAL_ID)
                        .param("status", "ACCEPTED")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AJAX 아이템 요청 — 포지션 필터 적용 후 해당 포지션 매칭만 반환")
    void listItems_ajaxPositionFilter_returnsOnlyMatchingsForPosition() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings/items", PROPOSAL_ID)
                        .param("positionId", "2")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ClientMatchingListItemViewModel> items = (List<ClientMatchingListItemViewModel>)
                result.getModelAndView().getModel().get("items");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).proposalPositionId()).isEqualTo(2L);
        assertThat(result.getModelAndView().getModel().get("detailBackUrl"))
                .isEqualTo("/proposals/10/matchings?positionId=2&backUrl=/proposals/10");
    }

    @Test
    @DisplayName("AJAX 아이템 요청 — 포지션+상태 복합 필터 적용")
    void listItems_ajaxCombinedFilter_returnsMatchingBothConditions() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings/items", PROPOSAL_ID)
                        .param("positionId", "1")
                        .param("status", "REJECTED")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ClientMatchingListItemViewModel> items = (List<ClientMatchingListItemViewModel>)
                result.getModelAndView().getModel().get("items");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).proposalPositionId()).isEqualTo(1L);
        assertThat(items.get(0).status()).isEqualTo(MatchingStatus.REJECTED);
        assertThat(result.getModelAndView().getModel().get("detailBackUrl"))
                .isEqualTo("/proposals/10/matchings?positionId=1&status=REJECTED&backUrl=/proposals/10");
    }

    @Test
    @DisplayName("AJAX 아이템 요청은 전달된 부모 backUrl까지 포함한 상세 복귀 경로를 만든다")
    void listItems_preservesParentBackUrlInDetailLink() throws Exception {
        Proposal proposal = createProposalWithTwoPositions();
        List<Matching> matchings = createMatchingsForBothPositions(proposal);

        given(proposalService.findOwnedProposal(PROPOSAL_ID, CLIENT_EMAIL)).willReturn(proposal);
        given(matchingRepository.findWithPositionAndFreelancerByProposalIdAndClientEmail(PROPOSAL_ID, CLIENT_EMAIL))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/{id}/matchings/items", PROPOSAL_ID)
                        .param("positionId", "1")
                        .param("status", "PROPOSED")
                        .param("backUrl", "/client/dashboard")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getModelAndView().getModel().get("detailBackUrl"))
                .isEqualTo("/proposals/10/matchings?positionId=1&status=PROPOSED&backUrl=/client/dashboard");
    }

    @Test
    @DisplayName("AJAX 아이템 요청 — 존재하지 않는 제안서는 예외 전파")
    void listItems_proposalNotFound_throwsException() {
        given(proposalService.findOwnedProposal(99L, CLIENT_EMAIL))
                .willThrow(new ProposalNotFoundException("제안서를 찾을 수 없습니다."));

        assertThatThrownBy(() ->
                mockMvc.perform(get("/proposals/99/matchings/items")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
        ).hasCauseInstanceOf(ProposalNotFoundException.class);
    }

    // ── 권한/예외 처리 ───────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 제안서 접근 시 대시보드로 리다이렉트")
    void list_proposalNotFound_redirectsToDashboard() throws Exception {
        given(proposalService.findOwnedProposal(99L, CLIENT_EMAIL))
                .willThrow(new ProposalNotFoundException("제안서를 찾을 수 없습니다."));

        mockMvc.perform(get("/proposals/99/matchings")
                        .with(authentication(authToken(CLIENT_EMAIL, "클라이언트"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attribute("errorMessage", "존재하지 않는 제안서입니다."));
    }

    @Test
    @DisplayName("타인 제안서 접근 시 대시보드로 리다이렉트")
    void list_accessDenied_redirectsToDashboard() throws Exception {
        given(proposalService.findOwnedProposal(PROPOSAL_ID, "other@example.com"))
                .willThrow(new AccessDeniedException("접근 불가"));

        mockMvc.perform(get("/proposals/{id}/matchings", PROPOSAL_ID)
                        .with(authentication(authToken("other@example.com", "타인"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attribute("errorMessage", "본인 제안서만 조회할 수 있습니다."));
    }

    // ── 헬퍼 ────────────────────────────────────────────────────

    private UsernamePasswordAuthenticationToken authToken(String email, String name) {
        ItDaPrincipal principal = ItDaPrincipal.from(
                createMember(email, "hashed-password", name, "010-1234-5678")
        );
        return new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private Proposal createProposalWithTwoPositions() {
        Member client = createMember(CLIENT_EMAIL, "pw", "클라이언트", "010-0000-0001");
        Proposal proposal = Proposal.create(client, "테스트 제안서", "", "설명", null, null, 8L);
        proposal.startMatching();
        ReflectionTestUtils.setField(proposal, "id", PROPOSAL_ID);

        Position backend = Position.create("백엔드");
        Position frontend = Position.create("프론트엔드");

        ProposalPosition pos1 = proposal.addPosition(backend, "백엔드", ProposalWorkType.REMOTE, 1L, 1_000_000L, 2_000_000L, 4L, null, null, null);
        ProposalPosition pos2 = proposal.addPosition(frontend, "프론트엔드", ProposalWorkType.REMOTE, 1L, 1_000_000L, 2_000_000L, 4L, null, null, null);
        ReflectionTestUtils.setField(pos1, "id", 1L);
        ReflectionTestUtils.setField(pos2, "id", 2L);
        return proposal;
    }

    private Proposal createProposalWithSinglePosition() {
        Member client = createMember(CLIENT_EMAIL, "pw", "클라이언트", "010-0000-0001");
        Proposal proposal = Proposal.create(client, "테스트 제안서", "", "설명", null, null, 8L);
        proposal.startMatching();
        ReflectionTestUtils.setField(proposal, "id", PROPOSAL_ID);

        Position backend = Position.create("백엔드");
        ProposalPosition pos = proposal.addPosition(backend, "백엔드", ProposalWorkType.REMOTE, 1L, 1_000_000L, 2_000_000L, 4L, null, null, null);
        ReflectionTestUtils.setField(pos, "id", 1L);
        return proposal;
    }

    private List<Matching> createMatchingsForBothPositions(Proposal proposal) {
        Member client = proposal.getMember();
        ProposalPosition pos1 = proposal.getPositions().stream().filter(p -> p.getId().equals(1L)).findFirst().orElseThrow();
        ProposalPosition pos2 = proposal.getPositions().stream().filter(p -> p.getId().equals(2L)).findFirst().orElseThrow();

        Matching m1 = Matching.create(null, pos1, client, createMember("f1@example.com", "pw", "프리1", "010-0000-0002"));
        Matching m2 = Matching.create(null, pos1, client, createMember("f2@example.com", "pw", "프리2", "010-0000-0003"));
        Matching m3 = Matching.create(null, pos2, client, createMember("f3@example.com", "pw", "프리3", "010-0000-0004"));
        ReflectionTestUtils.setField(m1, "id", 11L);
        ReflectionTestUtils.setField(m1, "status", MatchingStatus.PROPOSED);
        ReflectionTestUtils.setField(m2, "id", 12L);
        ReflectionTestUtils.setField(m2, "status", MatchingStatus.REJECTED);
        ReflectionTestUtils.setField(m3, "id", 21L);
        ReflectionTestUtils.setField(m3, "status", MatchingStatus.ACCEPTED);
        return List.of(m1, m2, m3);
    }

    private Matching createMatching(Long matchingId, MatchingStatus status, String freelancerName,
                                    Proposal proposal, LocalDateTime createdAt) {
        Member client = proposal.getMember();
        Member freelancer = createMember("f" + matchingId + "@example.com", "pw", freelancerName, "010-0000-0099");
        ProposalPosition pos = proposal.getPositions().iterator().next();

        Matching matching = Matching.create(null, pos, client, freelancer);
        ReflectionTestUtils.setField(matching, "id", matchingId);
        ReflectionTestUtils.setField(matching, "status", status);
        ReflectionTestUtils.setField(matching, "createdAt", createdAt);
        ReflectionTestUtils.setField(matching, "requestedAt", createdAt);
        return matching;
    }
}
