package com.generic4.itda.controller;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.generic4.itda.annotation.ControllerTest;
import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.exception.ProposalNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.matching.ClientMatchingFreelancerSelectionViewModel;
import com.generic4.itda.dto.matching.ClientMatchingPositionSelectionViewModel;
import com.generic4.itda.dto.security.ItDaPrincipal;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.service.ProposalService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ControllerTest(MatchingEntryController.class)
class MatchingEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProposalService proposalService;

    @MockitoBean
    private MatchingRepository matchingRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void selectPosition_rendersPositionsSortedAndCounts() throws Exception {
        Proposal proposal = createProposalWithThreePositions();
        given(proposalService.findOwnedProposal(10L, "client@example.com"))
                .willReturn(proposal);

        List<Matching> matchings = createMatchingsForPositions(proposal);
        given(matchingRepository.findByProposalPosition_Proposal_IdAndClientMember_Email_Value(10L, "client@example.com"))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/10/matchings")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("matching/positions"))
                .andReturn();

        ClientMatchingPositionSelectionViewModel viewModel = (ClientMatchingPositionSelectionViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(viewModel.proposalId()).isEqualTo(10L);
        assertThat(viewModel.openOnly()).isFalse();
        assertThat(viewModel.positions()).hasSize(3);

        // 정렬: OPEN -> FULL -> CLOSED
        assertThat(viewModel.positions().get(0).status()).isEqualTo(ProposalPositionStatus.OPEN);
        assertThat(viewModel.positions().get(1).status()).isEqualTo(ProposalPositionStatus.FULL);
        assertThat(viewModel.positions().get(2).status()).isEqualTo(ProposalPositionStatus.CLOSED);

        // 카운트: pos1(OPEN) = 2요청, 1활성 / pos2(FULL)=1요청, 1활성 / pos3(CLOSED)=0
        assertThat(viewModel.positions().get(0).requestedCount()).isEqualTo(2);
        assertThat(viewModel.positions().get(0).activeCount()).isEqualTo(1);
        assertThat(viewModel.positions().get(1).requestedCount()).isEqualTo(1);
        assertThat(viewModel.positions().get(1).activeCount()).isEqualTo(1);
        assertThat(viewModel.positions().get(2).requestedCount()).isZero();
        assertThat(viewModel.positions().get(2).activeCount()).isZero();
    }

    @Test
    void selectPosition_openOnly_filtersOpen() throws Exception {
        Proposal proposal = createProposalWithThreePositions();
        given(proposalService.findOwnedProposal(10L, "client@example.com"))
                .willReturn(proposal);

        List<Matching> matchings = createMatchingsForPositions(proposal);
        given(matchingRepository.findByProposalPosition_Proposal_IdAndClientMember_Email_Value(10L, "client@example.com"))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/10/matchings")
                        .param("openOnly", "true")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("matching/positions"))
                .andReturn();

        ClientMatchingPositionSelectionViewModel viewModel = (ClientMatchingPositionSelectionViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(viewModel.openOnly()).isTrue();
        assertThat(viewModel.positions()).hasSize(1);
        assertThat(viewModel.positions().get(0).status()).isEqualTo(ProposalPositionStatus.OPEN);
    }

    @Test
    void selectFreelancer_rendersFreelancersSortedAndFilterable() throws Exception {
        Proposal proposal = createProposalWithSinglePosition();
        given(proposalService.findOwnedProposal(10L, "client@example.com"))
                .willReturn(proposal);

        List<Matching> matchings = List.of(
                createMatching(101L, MatchingStatus.REJECTED, "프리랜서A", LocalDateTime.of(2026, 4, 22, 10, 0)),
                createMatching(103L, MatchingStatus.IN_PROGRESS, "프리랜서B", LocalDateTime.of(2026, 4, 22, 12, 0)),
                createMatching(102L, MatchingStatus.PROPOSED, "프리랜서C", LocalDateTime.of(2026, 4, 22, 11, 0))
        );
        given(matchingRepository.findWithFreelancerMemberByProposalPositionIdAndClientEmail(1L, "client@example.com"))
                .willReturn(matchings);

        MvcResult result = mockMvc.perform(get("/proposals/10/matchings/positions/1")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isOk())
                .andExpect(view().name("matching/freelancers"))
                .andReturn();

        ClientMatchingFreelancerSelectionViewModel viewModel = (ClientMatchingFreelancerSelectionViewModel)
                result.getModelAndView().getModel().get("view");

        assertThat(viewModel.proposalId()).isEqualTo(10L);
        assertThat(viewModel.proposalPositionId()).isEqualTo(1L);
        assertThat(viewModel.items()).hasSize(3);

        // 정렬: PROPOSED -> IN_PROGRESS -> REJECTED
        assertThat(viewModel.items().get(0).status()).isEqualTo(MatchingStatus.PROPOSED);
        assertThat(viewModel.items().get(1).status()).isEqualTo(MatchingStatus.IN_PROGRESS);
        assertThat(viewModel.items().get(2).status()).isEqualTo(MatchingStatus.REJECTED);

        MvcResult filtered = mockMvc.perform(get("/proposals/10/matchings/positions/1")
                        .param("status", "IN_PROGRESS")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().isOk())
                .andReturn();

        ClientMatchingFreelancerSelectionViewModel filteredView = (ClientMatchingFreelancerSelectionViewModel)
                filtered.getModelAndView().getModel().get("view");

        assertThat(filteredView.filterStatusKey()).isEqualTo("IN_PROGRESS");
        assertThat(filteredView.items()).hasSize(1);
        assertThat(filteredView.items().get(0).matchingId()).isEqualTo(103L);
    }

    // ── 포지션 선택: 권한/미존재 케이스 ────────────────────────

    @Test
    void selectPosition_proposalNotFound_redirectsToDashboard() throws Exception {
        given(proposalService.findOwnedProposal(99L, "client@example.com"))
                .willThrow(new ProposalNotFoundException("제안서를 찾을 수 없습니다."));

        mockMvc.perform(get("/proposals/99/matchings")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attribute("errorMessage", "존재하지 않는 제안서입니다."));
    }

    @Test
    void selectPosition_accessDenied_redirectsToDashboard() throws Exception {
        given(proposalService.findOwnedProposal(10L, "other@example.com"))
                .willThrow(new AccessDeniedException("접근 불가"));

        mockMvc.perform(get("/proposals/10/matchings")
                        .with(authentication(authToken("other@example.com", "타인"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attribute("errorMessage", "본인 제안서만 조회할 수 있습니다."));
    }

    // ── 프리랜서 선택: 권한/미존재 케이스 ───────────────────────

    @Test
    void selectFreelancer_proposalNotFound_redirectsToDashboard() throws Exception {
        given(proposalService.findOwnedProposal(99L, "client@example.com"))
                .willThrow(new ProposalNotFoundException("제안서를 찾을 수 없습니다."));

        mockMvc.perform(get("/proposals/99/matchings/positions/1")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attribute("errorMessage", "존재하지 않는 제안서입니다."));
    }

    @Test
    void selectFreelancer_accessDenied_redirectsToDashboard() throws Exception {
        given(proposalService.findOwnedProposal(10L, "other@example.com"))
                .willThrow(new AccessDeniedException("접근 불가"));

        mockMvc.perform(get("/proposals/10/matchings/positions/1")
                        .with(authentication(authToken("other@example.com", "타인"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/dashboard"))
                .andExpect(flash().attribute("errorMessage", "본인 제안서만 조회할 수 있습니다."));
    }

    @Test
    void selectFreelancer_invalidPositionId_redirectsToPositionList() throws Exception {
        Proposal proposal = createProposalWithSinglePosition(); // positionId=1만 있음
        given(proposalService.findOwnedProposal(10L, "client@example.com"))
                .willReturn(proposal);
        // proposalPositionId=999 는 proposal 안에 없으므로 repository 호출 없이 예외 발생

        mockMvc.perform(get("/proposals/10/matchings/positions/999")
                        .with(authentication(authToken("client@example.com", "클라이언트"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proposals/10/matchings"))
                .andExpect(flash().attribute("errorMessage", "잘못된 포지션입니다."));
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

    private Proposal createProposalWithThreePositions() {
        Member client = createMember("client@example.com", "pw", "클라이언트", "010-0000-0001");
        Proposal proposal = Proposal.create(client, "테스트 제안서", "", "설명", null, null, 8L);
        proposal.startMatching();
        ReflectionTestUtils.setField(proposal, "id", 10L);

        Position backend = Position.create("백엔드");
        Position frontend = Position.create("프론트엔드");
        Position design = Position.create("디자인");

        ProposalPosition pos1 = proposal.addPosition(backend, "백엔드", ProposalWorkType.REMOTE, 1L, 1_000_000L, 2_000_000L, 4L, null, null, null);
        ProposalPosition pos2 = proposal.addPosition(frontend, "프론트엔드", ProposalWorkType.REMOTE, 1L, 1_000_000L, 2_000_000L, 4L, null, null, null);
        ProposalPosition pos3 = proposal.addPosition(design, "디자인", ProposalWorkType.REMOTE, 1L, 1_000_000L, 2_000_000L, 4L, null, null, null);

        ReflectionTestUtils.setField(pos1, "id", 1L);
        ReflectionTestUtils.setField(pos2, "id", 2L);
        ReflectionTestUtils.setField(pos3, "id", 3L);

        pos2.changeStatus(ProposalPositionStatus.FULL);
        pos3.changeStatus(ProposalPositionStatus.CLOSED);
        return proposal;
    }

    private Proposal createProposalWithSinglePosition() {
        Member client = createMember("client@example.com", "pw", "클라이언트", "010-0000-0001");
        Proposal proposal = Proposal.create(client, "테스트 제안서", "", "설명", null, null, 8L);
        proposal.startMatching();
        ReflectionTestUtils.setField(proposal, "id", 10L);

        Position backend = Position.create("백엔드");
        ProposalPosition pos1 = proposal.addPosition(backend, "백엔드", ProposalWorkType.REMOTE, 1L, 1_000_000L, 2_000_000L, 4L, null, null, null);
        ReflectionTestUtils.setField(pos1, "id", 1L);
        return proposal;
    }

    private List<Matching> createMatchingsForPositions(Proposal proposal) {
        Member client = proposal.getMember();
        ProposalPosition pos1 = proposal.getPositions().stream().filter(p -> p.getId().equals(1L)).findFirst().orElseThrow();
        ProposalPosition pos2 = proposal.getPositions().stream().filter(p -> p.getId().equals(2L)).findFirst().orElseThrow();

        Matching openActive = Matching.create(null, pos1, client,
                createMember("freelancer1@example.com", "pw", "프리랜서1", "010-0000-0002"));
        ReflectionTestUtils.setField(openActive, "id", 11L);
        ReflectionTestUtils.setField(openActive, "status", MatchingStatus.PROPOSED);

        Matching openInactive = Matching.create(null, pos1, client,
                createMember("freelancer2@example.com", "pw", "프리랜서2", "010-0000-0003"));
        ReflectionTestUtils.setField(openInactive, "id", 12L);
        ReflectionTestUtils.setField(openInactive, "status", MatchingStatus.REJECTED);

        Matching fullActive = Matching.create(null, pos2, client,
                createMember("freelancer3@example.com", "pw", "프리랜서3", "010-0000-0004"));
        ReflectionTestUtils.setField(fullActive, "id", 21L);
        ReflectionTestUtils.setField(fullActive, "status", MatchingStatus.ACCEPTED);

        return List.of(openActive, openInactive, fullActive);
    }

    private Matching createMatching(Long matchingId, MatchingStatus status, String freelancerName, LocalDateTime createdAt) {
        Member client = createMember("client@example.com", "pw", "클라이언트", "010-0000-0001");
        Member freelancer = createMember("freelancer+" + matchingId + "@example.com", "pw", freelancerName, "010-0000-0002");

        Proposal proposal = Proposal.create(client, "테스트 제안서", "", "설명", null, null, 8L);
        proposal.startMatching();
        ReflectionTestUtils.setField(proposal, "id", 10L);

        Position backend = Position.create("백엔드");
        ProposalPosition position = proposal.addPosition(backend, "백엔드", ProposalWorkType.REMOTE, 1L, 1_000_000L, 2_000_000L, 4L, null, null, null);
        ReflectionTestUtils.setField(position, "id", 1L);

        Matching matching = Matching.create(null, position, client, freelancer);
        ReflectionTestUtils.setField(matching, "id", matchingId);
        ReflectionTestUtils.setField(matching, "status", status);
        ReflectionTestUtils.setField(matching, "createdAt", createdAt);
        ReflectionTestUtils.setField(matching, "requestedAt", createdAt);
        return matching;
    }
}
