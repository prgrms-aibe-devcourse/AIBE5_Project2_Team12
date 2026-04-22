package com.generic4.itda.service.recommend.reason;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecommendationReasonContextTest {

    @Test
    @DisplayName("추천 결과로부터 컨텍스트 생성 시 정보가 올바르게 매핑된다")
    void from_mapsFieldsCorrectly() {
        // given
        String proposalTitle = "백엔드 개발자 모집";
        String positionTitle = "Java 백엔드";
        BigDecimal finalScore = new BigDecimal("0.8500");
        ReasonFacts reasonFacts = new ReasonFacts(
                List.of("Java", "Spring"),
                List.of("E-commerce"),
                5,
                List.of("High performance code")
        );

        RecommendationResult result = mock(RecommendationResult.class);
        RecommendationRun run = mock(RecommendationRun.class);
        ProposalPosition position = mock(ProposalPosition.class);
        Proposal proposal = mock(Proposal.class);

        when(result.getRecommendationRun()).thenReturn(run);
        when(result.getReasonFacts()).thenReturn(reasonFacts);
        when(result.getFinalScore()).thenReturn(finalScore);
        when(run.getProposalPosition()).thenReturn(position);
        when(position.getProposal()).thenReturn(proposal);
        when(proposal.getTitle()).thenReturn(proposalTitle);
        when(position.getTitle()).thenReturn(positionTitle);

        // when
        RecommendationReasonContext context = RecommendationReasonContext.from(result);

        // then
        assertThat(context.proposalTitle()).isEqualTo(proposalTitle);
        assertThat(context.positionName()).isEqualTo(positionTitle);
        assertThat(context.reasonFacts()).isEqualTo(reasonFacts);
        assertThat(context.finalScore()).isEqualTo(finalScore);
    }

    @Test
    @DisplayName("추천 결과가 null이면 예외가 발생한다")
    void from_throwsException_whenResultIsNull() {
        assertThatThrownBy(() -> RecommendationReasonContext.from(null))
                .isInstanceOf(RuntimeException.class);
    }
}
