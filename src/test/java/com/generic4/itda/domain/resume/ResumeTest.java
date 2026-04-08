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

    private static Resume createResume(Member member, String introduction, Byte careerYears, CareerPayload career) {
        return Resume.builder()
                .member(member)
                .introduction(introduction)
                .careerYears(careerYears)
                .career(career)
                .preferredWorkType(WorkType.HYBRID)
                .publiclyVisible(true)
                .aiMatchingEnabled(true)
                .build();
    }

    private static CareerPayload createCareerPayload() {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("Generic4");
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setEndYearMonth(null);
        item.setCurrentlyWorking(true);
        item.setSummary("Spring Boot 기반 API를 개발하고 운영했습니다.");
        item.setTechStack(List.of("Java", "Spring Boot", "PostgreSQL"));

        CareerPayload payload = new CareerPayload();
        payload.setItems(List.of(item));
        return payload;
    }
}
