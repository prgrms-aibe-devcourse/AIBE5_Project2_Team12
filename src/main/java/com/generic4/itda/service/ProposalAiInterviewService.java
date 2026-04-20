package com.generic4.itda.service;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalAiInterviewMessage;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.dto.proposal.AiInterviewGenerateRequest;
import com.generic4.itda.dto.proposal.AiInterviewMessageResponse;
import com.generic4.itda.dto.proposal.AiInterviewResult;
import com.generic4.itda.dto.proposal.AiInterviewSendMessageResponse;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.exception.AiBriefGenerationException;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.ProposalAiInterviewMessageRepository;
import com.generic4.itda.repository.ProposalRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class ProposalAiInterviewService {

    private final ProposalRepository proposalRepository;
    private final ProposalAiInterviewMessageRepository messageRepository;
    private final AiInterviewGenerator aiInterviewGenerator;
    private final AiBriefProposalMapper aiBriefProposalMapper;

    public AiInterviewSendMessageResponse sendMessage(Long proposalId, String memberEmail, String userMessage) {
        Assert.notNull(proposalId, "제안서 id는 필수값입니다.");
        Assert.hasText(memberEmail, "회원 이메일은 필수값입니다.");
        Assert.hasText(userMessage, "AI 인터뷰 사용자 메시지는 필수값입니다.");

        Proposal proposal = getWritableProposalWithPositions(proposalId, memberEmail);
        List<ProposalAiInterviewMessage> previousMessages = findMessages(proposalId);
        String previousConversationText = toConversationText(previousMessages);

        ProposalAiInterviewMessage savedUserMessage = saveUserMessage(
                proposal,
                userMessage,
                nextSequence(previousMessages)
        );

        ProposalForm proposalFormBeforeAi = ProposalForm.from(proposal);
        AiInterviewResult aiInterviewResult = generateAiInterview(
                proposalFormBeforeAi,
                previousConversationText,
                savedUserMessage.getContent()
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiInterviewResult.getAiBriefResult(),
                savedUserMessage.getContent()
        );

        ProposalAiInterviewMessage savedAssistantMessage = saveAssistantMessage(
                proposal,
                aiInterviewResult.getAssistantMessage(),
                savedUserMessage.getSequence() + 1
        );

        List<ProposalAiInterviewMessage> currentMessages = new ArrayList<>(previousMessages);
        currentMessages.add(savedUserMessage);
        currentMessages.add(savedAssistantMessage);

        syncRawInputText(proposal, currentMessages);

        ProposalForm proposalForm = ProposalForm.from(proposal);
        List<AiInterviewMessageResponse> messageResponses = toMessageResponses(currentMessages);

        return AiInterviewSendMessageResponse.of(
                proposalForm,
                messageResponses,
                savedAssistantMessage.getContent()
        );
    }

    @Transactional(readOnly = true)
    public List<AiInterviewMessageResponse> findMessageResponses(Long proposalId, String memberEmail) {
        Assert.notNull(proposalId, "제안서 id는 필수값입니다.");
        Assert.hasText(memberEmail, "회원 이메일은 필수값입니다.");

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException("제안서를 찾을 수 없습니다. id=" + proposalId));

        validateOwnership(proposal, memberEmail);

        return toMessageResponses(findMessages(proposalId));
    }

    private Proposal getWritableProposalWithPositions(Long proposalId, String memberEmail) {
        Proposal proposal = proposalRepository.findWithPositionsById(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException("제안서를 찾을 수 없습니다. id=" + proposalId));

        validateOwnership(proposal, memberEmail);
        validateWritable(proposal);

        proposalRepository.findPositionsWithSkillsByProposalId(proposalId);
        return proposal;
    }

    private void validateOwnership(Proposal proposal, String memberEmail) {
        if (!proposal.getMember().getEmail().getValue().equals(memberEmail)) {
            throw new AccessDeniedException("본인 제안서에 대해서만 AI 인터뷰를 진행할 수 있습니다.");
        }
    }

    private void validateWritable(Proposal proposal) {
        if (proposal.getStatus() != ProposalStatus.WRITING) {
            throw new IllegalStateException("작성 중인 제안서만 AI 인터뷰를 진행할 수 있습니다.");
        }
    }

    private List<ProposalAiInterviewMessage> findMessages(Long proposalId) {
        return messageRepository.findAllByProposalIdOrderBySequenceAsc(proposalId);
    }

    private Integer nextSequence(List<ProposalAiInterviewMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 1;
        }
        return messages.get(messages.size() - 1).getSequence() + 1;
    }

    private ProposalAiInterviewMessage saveUserMessage(Proposal proposal, String content, Integer sequence) {
        ProposalAiInterviewMessage message = ProposalAiInterviewMessage.createUserMessage(
                proposal,
                content,
                sequence
        );
        return messageRepository.save(message);
    }

    private ProposalAiInterviewMessage saveAssistantMessage(Proposal proposal, String content, Integer sequence) {
        ProposalAiInterviewMessage message = ProposalAiInterviewMessage.createAssistantMessage(
                proposal,
                content,
                sequence
        );
        return messageRepository.save(message);
    }

    private AiInterviewResult generateAiInterview(
            ProposalForm proposalForm,
            String previousConversationText,
            String userMessage
    ) {
        try {
            return aiInterviewGenerator.generate(
                    AiInterviewGenerateRequest.of(
                            proposalForm,
                            previousConversationText,
                            userMessage
                    )
            );
        } catch (AiBriefGenerationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiBriefGenerationException("AI 인터뷰 생성에 실패했습니다.", exception);
        }
    }

    private void syncRawInputText(Proposal proposal, List<ProposalAiInterviewMessage> messages) {
        String conversationText = toConversationText(messages);
        String rawInputText = StringUtils.hasText(conversationText) ? conversationText : proposal.getRawInputText();

        proposal.update(
                proposal.getTitle(),
                rawInputText,
                proposal.getDescription(),
                proposal.getTotalBudgetMin(),
                proposal.getTotalBudgetMax(),
                proposal.getExpectedPeriod()
        );
    }

    private String toConversationText(List<ProposalAiInterviewMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        return messages.stream()
                .map(ProposalAiInterviewMessage::toRawInputLine)
                .toList()
                .stream()
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private List<AiInterviewMessageResponse> toMessageResponses(List<ProposalAiInterviewMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        return messages.stream()
                .map(AiInterviewMessageResponse::from)
                .toList();
    }
}