package com.generic4.itda.domain.proposal;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.member.UserStatus;
import com.generic4.itda.domain.position.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class ProposalTest {

    @DisplayName("유효한 입력이 주어지면 작성 중 상태의 제안서를 생성한다")
    @Test
    void createWithValidInputs() {
        Member member = createMember();

        Proposal proposal = Proposal.create(
                member,
                "  AI 기반 매칭 플랫폼 구축  ",
                "원본 자유 입력",
                "  최종 제안서 본문  ",
                1_000_000L,
                3_000_000L,
                ProposalWorkType.HYBRID,
                "  서울 강남구  ",
                12L
        );

        assertThat(proposal.getMember()).isEqualTo(member);
        assertThat(proposal.getTitle()).isEqualTo("AI 기반 매칭 플랫폼 구축");
        assertThat(proposal.getRawInputText()).isEqualTo("원본 자유 입력");
        assertThat(proposal.getDescription()).isEqualTo("최종 제안서 본문");
        assertThat(proposal.getTotalBudgetMin()).isEqualTo(1_000_000L);
        assertThat(proposal.getTotalBudgetMax()).isEqualTo(3_000_000L);
        assertThat(proposal.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(proposal.getWorkPlace()).isEqualTo("서울 강남구");
        assertThat(proposal.getExpectedPeriod()).isEqualTo(12L);
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.WRITING);
    }

    @DisplayName("raw input text는 앞뒤 공백을 보존한다")
    @Test
    void preserveRawInputText() {
        Proposal proposal = createProposal("  원본 입력  ");

        assertThat(proposal.getRawInputText()).isEqualTo("  원본 입력  ");
    }

    @DisplayName("비활성 회원은 제안서를 생성할 수 없다")
    @Test
    void failWhenMemberIsInactive() {
        Member member = createMember();
        ReflectionTestUtils.setField(member, "status", UserStatus.INACTIVE);

        assertThatThrownBy(() -> Proposal.create(
                member,
                "제안서 제목",
                "원본 입력",
                null,
                null,
                null,
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("활성 회원만 제안서를 작성할 수 있습니다.");
    }

    @DisplayName("제안서 제목이 없으면 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenTitleIsMissing(String title) {
        assertThatThrownBy(() -> Proposal.create(
                createMember(),
                title,
                "원본 입력",
                null,
                null,
                null,
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("제안서 제목은 필수값입니다.");
    }

    @DisplayName("원본 입력이 없으면 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenRawInputTextIsMissing(String rawInputText) {
        assertThatThrownBy(() -> Proposal.create(
                createMember(),
                "제안서 제목",
                rawInputText,
                null,
                null,
                null,
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("제안서 원본 입력은 필수값입니다.");
    }

    @DisplayName("전체 예산 범위가 뒤집히면 생성에 실패한다")
    @Test
    void failWhenBudgetRangeIsInvalid() {
        assertThatThrownBy(() -> Proposal.create(
                createMember(),
                "제안서 제목",
                "원본 입력",
                null,
                5_000_000L,
                1_000_000L,
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("전체 최소 예산은 최대 예산보다 클 수 없습니다.");
    }

    @DisplayName("예상 기간이 0 이하이면 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void failWhenExpectedPeriodIsNotPositive(long expectedPeriod) {
        assertThatThrownBy(() -> Proposal.create(
                createMember(),
                "제안서 제목",
                "원본 입력",
                null,
                null,
                null,
                null,
                null,
                expectedPeriod
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예상 기간은 양수여야 합니다.");
    }

    @DisplayName("유효한 입력이면 제안서를 수정한다")
    @Test
    void updateProposal() {
        Proposal proposal = createProposal("원본 입력");

        proposal.update(
                "  수정된 제목  ",
                "새 원본 입력",
                "  수정된 본문  ",
                2_000_000L,
                4_000_000L,
                ProposalWorkType.REMOTE,
                "  판교  ",
                24L
        );

        assertThat(proposal.getTitle()).isEqualTo("수정된 제목");
        assertThat(proposal.getRawInputText()).isEqualTo("새 원본 입력");
        assertThat(proposal.getDescription()).isEqualTo("수정된 본문");
        assertThat(proposal.getTotalBudgetMin()).isEqualTo(2_000_000L);
        assertThat(proposal.getTotalBudgetMax()).isEqualTo(4_000_000L);
        assertThat(proposal.getWorkType()).isEqualTo(ProposalWorkType.REMOTE);
        assertThat(proposal.getWorkPlace()).isEqualTo("판교");
        assertThat(proposal.getExpectedPeriod()).isEqualTo(24L);
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.WRITING);
    }

    @DisplayName("작성 중인 제안서는 매칭 상태로 전환할 수 있다")
    @Test
    void startMatching() {
        Proposal proposal = createProposal("원본 입력");

        proposal.startMatching();

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.MATCHING);
    }

    @DisplayName("매칭 중인 제안서는 종료 상태로 전환할 수 있다")
    @Test
    void completeProposal() {
        Proposal proposal = createProposal("원본 입력");
        proposal.startMatching();

        proposal.complete();

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.COMPLETE);
    }

    @DisplayName("작성 중이 아닌 제안서는 매칭을 시작할 수 없다")
    @Test
    void failWhenStartMatchingFromNonWritingStatus() {
        Proposal proposal = createProposal("원본 입력");
        proposal.startMatching();

        assertThatThrownBy(proposal::startMatching)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("작성 중인 제안서만 매칭을 시작할 수 있습니다.");
    }

    @DisplayName("매칭 중이 아닌 제안서는 종료할 수 없다")
    @Test
    void failWhenCompleteFromNonMatchingStatus() {
        Proposal proposal = createProposal("원본 입력");

        assertThatThrownBy(proposal::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("매칭 중인 제안서만 종료할 수 있습니다.");
    }

    @DisplayName("제안서에 모집 단위를 추가하고 제거할 수 있다")
    @Test
    void addAndRemovePosition() {
        Proposal proposal = createProposal("원본 입력");
        ProposalPosition proposalPosition = proposal.addPosition(Position.create("백엔드 개발자"), 2L, 3_000_000L, 5_000_000L);

        assertThat(proposal.getPositions()).hasSize(1);
        assertThat(proposalPosition.getProposal()).isEqualTo(proposal);

        proposal.removePosition(proposalPosition);

        assertThat(proposal.getPositions()).isEmpty();
        assertThat(proposalPosition.getProposal()).isNull();
    }

    @DisplayName("같은 직무 마스터를 같은 제안서 안에 여러 줄로 둘 수 있다")
    @Test
    void allowDuplicatedPositionMasterWithinProposal() {
        Proposal proposal = createProposal("원본 입력");
        Position backend = Position.create("백엔드 개발자");

        proposal.addPosition(backend, 1L, 3_000_000L, 4_000_000L);
        proposal.addPosition(backend, 2L, 4_000_000L, 6_000_000L);

        assertThat(proposal.getPositions()).hasSize(2);
        assertThat(proposal.getPositions())
                .extracting(ProposalPosition::getPosition)
                .containsExactly(backend, backend);
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
                8L
        );
    }
}
