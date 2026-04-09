package com.generic4.itda.domain.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SkillTest {

    @DisplayName("유효한 입력이 주어지면 스킬을 생성한다")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validSkillSource")
    void createWithValidInputs(String name, String description, String expectedName, String expectedDescription) {
        Skill skill = createSkill(name, description);

        assertThat(skill.getName()).isEqualTo(expectedName);
        assertThat(skill.getDescription()).isEqualTo(expectedDescription);
    }

    @DisplayName("스킬 이름이 누락되면 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenNameIsMissing(String name) {
        assertThatThrownBy(() -> createSkill(name, "설명"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스킬 이름은 필수값입니다.");
    }

    @DisplayName("유효한 입력이 주어지면 스킬을 수정한다")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validSkillSource")
    void updateWithValidInputs(String name, String description, String expectedName, String expectedDescription) {
        Skill skill = createSkill("Java", "기본 설명");

        skill.update(name, description);

        assertThat(skill.getName()).isEqualTo(expectedName);
        assertThat(skill.getDescription()).isEqualTo(expectedDescription);
    }

    @DisplayName("수정 시 스킬 이름이 누락되면 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenUpdateNameIsMissing(String name) {
        Skill skill = createSkill("Java", "기본 설명");

        assertThatThrownBy(() -> skill.update(name, "설명"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스킬 이름은 필수값입니다.");
    }

    @DisplayName("설명이 공백이면 null로 정규화한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void normalizeBlankDescriptionToNull(String description) {
        Skill skill = createSkill("Java", description);

        assertThat(skill.getDescription()).isNull();

        skill.update("Spring", description);

        assertThat(skill.getDescription()).isNull();
    }

    private static Skill createSkill(String name, String description) {
        return Skill.create(name, description);
    }

    private static Stream<Arguments> validSkillSource() {
        return Stream.of(
                Arguments.of(
                        Named.of("plain values", "Java"),
                        "백엔드 언어",
                        "Java",
                        "백엔드 언어"
                ),
                Arguments.of(
                        Named.of("trim values", "  Spring Boot  "),
                        "  웹 프레임워크  ",
                        "Spring Boot",
                        "웹 프레임워크"
                ),
                Arguments.of(
                        Named.of("null description", "JPA"),
                        null,
                        "JPA",
                        null
                )
        );
    }
}
