package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalAiInterviewMessage;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.proposal.AiInterviewGenerateRequest;
import com.generic4.itda.dto.proposal.AiInterviewResult;
import com.generic4.itda.dto.proposal.AiInterviewSendMessageResponse;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.ProposalAiInterviewMessageRepository;
import com.generic4.itda.repository.ProposalRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProposalAiInterviewServiceTest {

    private static final String OWNER_EMAIL = "test@example.com";

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ProposalAiInterviewMessageRepository messageRepository;

    @Mock
    private AiInterviewGenerator aiInterviewGenerator;

    @Mock
    private AiBriefProposalMapper aiBriefProposalMapper;

    @InjectMocks
    private ProposalAiInterviewService proposalAiInterviewService;

    @Captor
    private ArgumentCaptor<AiInterviewGenerateRequest> generateRequestCaptor;

    @Captor
    private ArgumentCaptor<ProposalAiInterviewMessage> messageCaptor;

    @Test
    @DisplayName("사용자 메시지를 저장하고 AI 응답 메시지와 갱신된 제안서 폼을 반환한다")
    void sendMessageSavesUserAndAssistantMessagesAndReturnsUpdatedForm() {
        Proposal proposal = createProposal();
        AiBriefResult aiBriefResult = AiBriefResult.of(
                "온라인 쇼핑몰 구축",
                "온라인 쇼핑몰 웹사이트를 구축합니다.",
                null,
                null,
                null,
                null
        );
        AiInterviewResult aiInterviewResult = AiInterviewResult.of(
                aiBriefResult,
                "쇼핑몰에 필요한 주요 기능을 알려주세요. 결제, 회원가입, 관리자 페이지가 필요할까요?"
        );

        given(proposalRepository.findWithPositionsById(1L)).willReturn(Optional.of(proposal));
        given(proposalRepository.findPositionsWithSkillsByProposalId(1L)).willReturn(List.of());
        given(messageRepository.findAllByProposalIdOrderBySequenceAsc(1L)).willReturn(List.of());
        given(messageRepository.save(any(ProposalAiInterviewMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(aiInterviewGenerator.generate(any(AiInterviewGenerateRequest.class))).willReturn(aiInterviewResult);

        AiInterviewSendMessageResponse response = proposalAiInterviewService.sendMessage(
                1L,
                OWNER_EMAIL,
                "온라인 쇼핑몰 웹사이트를 만들고 싶어요."
        );

        then(messageRepository).should(times(2)).save(messageCaptor.capture());
        List<ProposalAiInterviewMessage> savedMessages = messageCaptor.getAllValues();

        assertThat(savedMessages).hasSize(2);
        assertThat(savedMessages.get(0).getRole().name()).isEqualTo("USER");
        assertThat(savedMessages.get(0).getContent()).isEqualTo("온라인 쇼핑몰 웹사이트를 만들고 싶어요.");
        assertThat(savedMessages.get(0).getSequence()).isEqualTo(1);
        assertThat(savedMessages.get(1).getRole().name()).isEqualTo("ASSISTANT");
        assertThat(savedMessages.get(1).getContent()).isEqualTo("쇼핑몰에 필요한 주요 기능을 알려주세요. 결제, 회원가입, 관리자 페이지가 필요할까요?");
        assertThat(savedMessages.get(1).getSequence()).isEqualTo(2);

        then(aiInterviewGenerator).should().generate(generateRequestCaptor.capture());
        assertThat(generateRequestCaptor.getValue().getConversationText()).isEmpty();
        assertThat(generateRequestCaptor.getValue().getUserMessage()).isEqualTo("온라인 쇼핑몰 웹사이트를 만들고 싶어요.");

        then(aiBriefProposalMapper).should().applyForInterview(
                proposal,
                aiBriefResult,
                "온라인 쇼핑몰 웹사이트를 만들고 싶어요."
        );

        assertThat(response.getProposalForm().getRawInputText()).isEqualTo(
                "[USER] 온라인 쇼핑몰 웹사이트를 만들고 싶어요." + System.lineSeparator()
                        + "[ASSISTANT] 쇼핑몰에 필요한 주요 기능을 알려주세요. 결제, 회원가입, 관리자 페이지가 필요할까요?"
        );
        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getAssistantMessage()).isEqualTo("쇼핑몰에 필요한 주요 기능을 알려주세요. 결제, 회원가입, 관리자 페이지가 필요할까요?");
    }

    @Test
    @DisplayName("기존 메시지가 있으면 다음 sequence로 사용자 메시지와 AI 메시지를 저장한다")
    void sendMessageUsesNextSequenceWhenPreviousMessagesExist() {
        Proposal proposal = createProposal();
        ProposalAiInterviewMessage previousUserMessage = ProposalAiInterviewMessage.createUserMessage(
                proposal,
                "온라인 쇼핑몰 웹사이트를 만들고 싶어요.",
                1
        );
        ProposalAiInterviewMessage previousAssistantMessage = ProposalAiInterviewMessage.createAssistantMessage(
                proposal,
                "필요한 포지션을 알려주세요.",
                2
        );
        AiBriefResult aiBriefResult = AiBriefResult.of(
                "온라인 쇼핑몰 구축",
                "온라인 쇼핑몰 웹사이트를 구축합니다.",
                null,
                null,
                null,
                null
        );
        AiInterviewResult aiInterviewResult = AiInterviewResult.of(
                aiBriefResult,
                "백엔드 개발자 2명과 프론트엔드 개발자 1명으로 반영했습니다. 예상 기간은 몇 주인가요?"
        );

        given(proposalRepository.findWithPositionsById(1L)).willReturn(Optional.of(proposal));
        given(proposalRepository.findPositionsWithSkillsByProposalId(1L)).willReturn(List.of());
        given(messageRepository.findAllByProposalIdOrderBySequenceAsc(1L))
                .willReturn(List.of(previousUserMessage, previousAssistantMessage));
        given(messageRepository.save(any(ProposalAiInterviewMessage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(aiInterviewGenerator.generate(any(AiInterviewGenerateRequest.class))).willReturn(aiInterviewResult);

        AiInterviewSendMessageResponse response = proposalAiInterviewService.sendMessage(
                1L,
                OWNER_EMAIL,
                "백엔드 개발자 2명, 프론트엔드 개발자 1명으로 갈게요."
        );

        then(messageRepository).should(times(2)).save(messageCaptor.capture());
        List<ProposalAiInterviewMessage> savedMessages = messageCaptor.getAllValues();

        assertThat(savedMessages.get(0).getSequence()).isEqualTo(3);
        assertThat(savedMessages.get(0).getRole().name()).isEqualTo("USER");
        assertThat(savedMessages.get(1).getSequence()).isEqualTo(4);
        assertThat(savedMessages.get(1).getRole().name()).isEqualTo("ASSISTANT");

        then(aiInterviewGenerator).should().generate(generateRequestCaptor.capture());
        assertThat(generateRequestCaptor.getValue().getConversationText()).isEqualTo(
                "[USER] 온라인 쇼핑몰 웹사이트를 만들고 싶어요." + System.lineSeparator()
                        + "[ASSISTANT] 필요한 포지션을 알려주세요."
        );
        assertThat(generateRequestCaptor.getValue().getUserMessage()).isEqualTo("백엔드 개발자 2명, 프론트엔드 개발자 1명으로 갈게요.");

        then(aiBriefProposalMapper).should().applyForInterview(
                proposal,
                aiBriefResult,
                "백엔드 개발자 2명, 프론트엔드 개발자 1명으로 갈게요."
        );

        assertThat(response.getMessages()).hasSize(4);
    }

    @Test
    @DisplayName("저장된 메시지 히스토리를 조회한다")
    void findMessageResponses() {
        Proposal proposal = createProposal();
        ProposalAiInterviewMessage userMessage = ProposalAiInterviewMessage.createUserMessage(
                proposal,
                "온라인 쇼핑몰 웹사이트를 만들고 싶어요.",
                1
        );
        ProposalAiInterviewMessage assistantMessage = ProposalAiInterviewMessage.createAssistantMessage(
                proposal,
                "필요한 포지션을 알려주세요.",
                2
        );

        given(proposalRepository.findById(1L)).willReturn(Optional.of(proposal));
        given(messageRepository.findAllByProposalIdOrderBySequenceAsc(1L))
                .willReturn(List.of(userMessage, assistantMessage));

        var responses = proposalAiInterviewService.findMessageResponses(1L, OWNER_EMAIL);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getRole()).isEqualTo("USER");
        assertThat(responses.get(0).getContent()).isEqualTo("온라인 쇼핑몰 웹사이트를 만들고 싶어요.");
        assertThat(responses.get(0).getSequence()).isEqualTo(1);
        assertThat(responses.get(1).getRole()).isEqualTo("ASSISTANT");
        assertThat(responses.get(1).getContent()).isEqualTo("필요한 포지션을 알려주세요.");
        assertThat(responses.get(1).getSequence()).isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 제안서이면 AI 인터뷰 메시지 전송을 중단한다")
    void failWhenProposalDoesNotExist() {
        given(proposalRepository.findWithPositionsById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> proposalAiInterviewService.sendMessage(1L, OWNER_EMAIL, "메시지"))
                .isInstanceOf(ProposalNotFoundException.class)
                .hasMessage("제안서를 찾을 수 없습니다. id=1");

        then(messageRepository).should(never()).save(any());
        then(aiInterviewGenerator).should(never()).generate(any());
        then(aiBriefProposalMapper).should(never()).applyForInterview(any(), any(), any());
    }

    @Test
    @DisplayName("본인 제안서가 아니면 AI 인터뷰 메시지 전송을 거부한다")
    void failWhenProposalOwnerDoesNotMatch() {
        Proposal proposal = createProposal();
        given(proposalRepository.findWithPositionsById(1L)).willReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalAiInterviewService.sendMessage(1L, "other@example.com", "메시지"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("본인 제안서에 대해서만 AI 인터뷰를 진행할 수 있습니다.");

        then(messageRepository).should(never()).save(any());
        then(aiInterviewGenerator).should(never()).generate(any());
        then(aiBriefProposalMapper).should(never()).applyForInterview(any(), any(), any());
    }

    @Test
    @DisplayName("작성 중이 아닌 제안서는 AI 인터뷰 메시지를 전송할 수 없다")
    void failWhenProposalStatusIsNotWriting() {
        Proposal proposal = createProposal();
        proposal.startMatching();
        given(proposalRepository.findWithPositionsById(1L)).willReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalAiInterviewService.sendMessage(1L, OWNER_EMAIL, "메시지"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("작성 중인 제안서만 AI 인터뷰를 진행할 수 있습니다.");

        then(messageRepository).should(never()).save(any());
        then(aiInterviewGenerator).should(never()).generate(any());
        then(aiBriefProposalMapper).should(never()).applyForInterview(any(), any(), any());
    }

    private Proposal createProposal() {
        return Proposal.create(
                createMember(OWNER_EMAIL, "hashed-password", "클라이언트", "010-1234-5678"),
                "제안서 제목",
                "",
                "제안서 본문",
                1_000_000L,
                2_000_000L,
                3L
        );
    }
}