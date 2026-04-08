package com.generic4.itda.domain.resume;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.member.Member;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ResumeTest {

    @DisplayName("유효한 입력이 주어지면 이력서를 생성한다")
    @ParameterizedTest
    @ValueSource(bytes = {0, 3})
    void createWithValidInputs(byte careerYears) {
        Member member = createMember();

        Resume resume = createResume(
                member,
                "백엔드 개발자입니다.",
                careerYears,
                createCareerPayload()
        );

        assertThat(resume.getMember()).isEqualTo(member);
        assertThat(resume.getIntroduction()).isEqualTo("백엔드 개발자입니다.");
        assertThat(resume.getCareerYears()).isEqualTo(careerYears);
        assertThat(resume.getCareer().getItems()).hasSize(1);
        assertThat(resume.getCareer().getItems().get(0).getCompanyName()).isEqualTo("Generic4");
        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.HYBRID);
        assertThat(resume.isPubliclyVisible()).isTrue();
        assertThat(resume.isAiMatchingEnabled()).isTrue();
    }

    @DisplayName("선호 근무 형태와 노출 설정이 누락되면 기본값으로 이력서를 생성한다")
    @Test
    void createWithDefaultOptionsWhenOptionalInputsAreNull() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                null,
                null,
                null
        );

        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.SITE);
        assertThat(resume.isPubliclyVisible()).isTrue();
        assertThat(resume.isAiMatchingEnabled()).isTrue();
    }

    @DisplayName("공개 여부와 AI 매칭 허용 여부는 명시한 값을 유지한다")
    @Test
    void createWithExplicitVisibilityAndMatchingOptions() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                false,
                false
        );

        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.HYBRID);
        assertThat(resume.isPubliclyVisible()).isFalse();
        assertThat(resume.isAiMatchingEnabled()).isFalse();
    }

    @DisplayName("회원이 누락되면 이력서 생성에 실패한다")
    @Test
    void failWhenMemberIsNull() {
        assertThatThrownBy(() -> createResume(
                null,
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("회원은 필수값입니다.");
    }

    @DisplayName("자기소개가 누락되면 이력서 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenIntroductionIsMissing(String introduction) {
        assertThatThrownBy(() -> createResume(
                createMember(),
                introduction,
                (byte) 3,
                createCareerPayload()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기소개는 필수값입니다.");
    }

    @DisplayName("경력 정보가 누락되면 이력서 생성에 실패한다")
    @Test
    void failWhenCareerIsMissing() {
        assertThatThrownBy(() -> createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력은 필수값입니다.");
    }

    @DisplayName("경력 연차가 누락되면 이력서 생성에 실패한다")
    @Test
    void failWhenCareerYearsIsNull() {
        assertThatThrownBy(() -> createResume(
                createMember(),
                "백엔드 개발자입니다.",
                null,
                createCareerPayload()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력 연차는 필수값입니다.");
    }

    @DisplayName("경력 연차가 음수이면 이력서 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(bytes = {-1, -10})
    void failWhenCareerYearsIsNegative(byte careerYears) {
        assertThatThrownBy(() -> createResume(
                createMember(),
                "백엔드 개발자입니다.",
                careerYears,
                createCareerPayload()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력 연차는 음수일 수 없습니다.");
    }

    @DisplayName("유효한 입력이 주어지면 이력서를 수정한다")
    @Test
    void updateWithValidInputs() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        CareerPayload updatedCareer = createCareerPayload("OpenAI", "대규모 트래픽 환경에서 API를 운영했습니다.");

        resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 5,
                updatedCareer,
                WorkType.REMOTE
        );

        assertThat(resume.getIntroduction()).isEqualTo("플랫폼 백엔드 개발자입니다.");
        assertThat(resume.getCareerYears()).isEqualTo((byte) 5);
        assertThat(resume.getCareer()).isEqualTo(updatedCareer);
        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.REMOTE);
    }

    @DisplayName("수정 시 선호 근무 형태가 누락되면 SITE를 기본값으로 사용한다")
    @Test
    void updateWithDefaultWorkTypeWhenNull() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                true,
                true
        );

        resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                createCareerPayload("Generic4", "배치 시스템을 개선했습니다."),
                null
        );

        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.SITE);
    }

    @DisplayName("수정 시 자기소개가 누락되면 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenUpdateIntroductionIsMissing(String introduction) {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        assertThatThrownBy(() -> resume.update(
                introduction,
                (byte) 4,
                createCareerPayload(),
                WorkType.HYBRID
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기소개는 필수값입니다.");
    }

    @DisplayName("수정 시 경력 정보가 누락되면 실패한다")
    @Test
    void failWhenUpdateCareerIsMissing() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        assertThatThrownBy(() -> resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                null,
                WorkType.HYBRID
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력은 필수값입니다.");
    }

    @DisplayName("수정 시 경력 연차가 누락되면 실패한다")
    @Test
    void failWhenUpdateCareerYearsIsNull() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        assertThatThrownBy(() -> resume.update(
                "플랫폼 백엔드 개발자입니다.",
                null,
                createCareerPayload(),
                WorkType.HYBRID
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력 연차는 필수값입니다.");
    }

    @DisplayName("수정 시 경력 연차가 음수이면 실패한다")
    @ParameterizedTest
    @ValueSource(bytes = {-1, -10})
    void failWhenUpdateCareerYearsIsNegative(byte careerYears) {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        assertThatThrownBy(() -> resume.update(
                "플랫폼 백엔드 개발자입니다.",
                careerYears,
                createCareerPayload(),
                WorkType.HYBRID
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력 연차는 음수일 수 없습니다.");
    }

    @DisplayName("공개 여부 토글 시 상태가 반전된다")
    @Test
    void togglePubliclyVisible() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                true,
                true
        );

        resume.togglePubliclyVisible();

        assertThat(resume.isPubliclyVisible()).isFalse();
    }

    @DisplayName("AI 매칭 허용 여부 토글 시 상태가 반전된다")
    @Test
    void toggleAiMatchingEnabled() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                true,
                true
        );

        resume.toggleAiMatchingEnabled();

        assertThat(resume.isAiMatchingEnabled()).isFalse();
    }

    private static Resume createResume(Member member, String introduction, Byte careerYears, CareerPayload career) {
        return createResume(member, introduction, careerYears, career, WorkType.HYBRID, true, true);
    }

    private static Resume createResume(Member member, String introduction, Byte careerYears, CareerPayload career,
            WorkType preferredWorkType, Boolean publiclyVisible, Boolean aiMatchingEnabled) {
        return Resume.create(
                member,
                introduction,
                careerYears,
                career,
                preferredWorkType,
                publiclyVisible,
                aiMatchingEnabled
        );
    }

    private static CareerPayload createCareerPayload() {
        return createCareerPayload("Generic4", "Spring Boot 기반 API를 개발하고 운영했습니다.");
    }

    private static CareerPayload createCareerPayload(String companyName, String summary) {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName(companyName);
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setEndYearMonth(null);
        item.setCurrentlyWorking(true);
        item.setSummary(summary);
        item.setTechStack(List.of("Java", "Spring Boot", "PostgreSQL"));

        CareerPayload payload = new CareerPayload();
        payload.setItems(List.of(item));
        return payload;
    }
}
