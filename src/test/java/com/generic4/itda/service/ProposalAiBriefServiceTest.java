package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.proposal.AiBriefGenerateRequest;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.exception.AiBriefGenerationException;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.ProposalRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProposalAiBriefServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private AiBriefGenerator aiBriefGenerator;

    @Mock
    private AiBriefProposalMapper aiBriefProposalMapper;

    @InjectMocks
    private ProposalAiBriefService proposalAiBriefService;

    @Captor
    private ArgumentCaptor<AiBriefGenerateRequest> requestCaptor;

    @Test
    @DisplayName("작성 중인 제안서의 raw input text로 AI 브리프를 생성하고 결과를 반영한다")
    void generateAiBriefAndApplyToProposal() {
        Proposal proposal = createProposal("AI 브리프용 원본 입력");
        AiBriefResult aiBriefResult = AiBriefResult.of(
                "AI가 만든 제목",
                "AI가 만든 설명",
                3_000_000L,
                5_000_000L,
                ProposalWorkType.REMOTE,
                "판교",
                6L,
                null
        );

        given(proposalRepository.findById(1L)).willReturn(Optional.of(proposal));
        given(aiBriefGenerator.generate(any(AiBriefGenerateRequest.class))).willReturn(aiBriefResult);

        AiBriefResult result = proposalAiBriefService.generate(1L);

        assertThat(result).isSameAs(aiBriefResult);
        then(aiBriefGenerator).should().generate(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getRawInputText()).isEqualTo("AI 브리프용 원본 입력");
        then(aiBriefProposalMapper).should().apply(proposal, aiBriefResult);
    }

    @Test
    @DisplayName("존재하지 않는 제안서이면 AI 브리프 생성을 중단한다")
    void failWhenProposalDoesNotExist() {
        given(proposalRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> proposalAiBriefService.generate(1L))
                .isInstanceOf(ProposalNotFoundException.class)
                .hasMessage("제안서를 찾을 수 없습니다. id=1");

        then(aiBriefGenerator).should(never()).generate(any());
        then(aiBriefProposalMapper).should(never()).apply(any(), any());
    }

    @Test
    @DisplayName("작성 중이 아닌 제안서는 AI 브리프를 생성할 수 없다")
    void failWhenProposalStatusIsNotWriting() {
        Proposal proposal = createProposal("원본 입력");
        proposal.startMatching();
        given(proposalRepository.findById(1L)).willReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalAiBriefService.generate(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("작성 중인 제안서만 AI 브리프를 생성할 수 있습니다.");

        then(aiBriefGenerator).should(never()).generate(any());
        then(aiBriefProposalMapper).should(never()).apply(any(), any());
    }

    @Test
    @DisplayName("AI 생성기가 예외를 던지면 Proposal 변경 없이 AiBriefGenerationException으로 감싼다")
    void failWhenAiGeneratorThrowsException() {
        Proposal proposal = createProposal("실패용 원본 입력");
        given(proposalRepository.findById(1L)).willReturn(Optional.of(proposal));
        given(aiBriefGenerator.generate(any(AiBriefGenerateRequest.class)))
                .willThrow(new RuntimeException("llm failure"));

        assertThatThrownBy(() -> proposalAiBriefService.generate(1L))
                .isInstanceOf(AiBriefGenerationException.class)
                .hasMessage("AI 브리프 생성에 실패했습니다.")
                .cause()
                .isInstanceOf(RuntimeException.class)
                .hasMessage("llm failure");

        assertThat(proposal.getTitle()).isEqualTo("제안서 제목");
        assertThat(proposal.getDescription()).isEqualTo("제안서 본문");
        then(aiBriefProposalMapper).should(never()).apply(any(), any());
    }

    private Proposal createProposal(String rawInputText) {
        return Proposal.create(
                createMember(),
                "제안서 제목",
                rawInputText,
                "제안서 본문",
                1_000_000L,
                2_000_000L,
                ProposalWorkType.SITE,
                "서울",
                3L
        );
    }
}
