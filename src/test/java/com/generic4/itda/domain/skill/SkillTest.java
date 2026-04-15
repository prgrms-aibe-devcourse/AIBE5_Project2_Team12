package com.generic4.itda.domain.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("같은 참조이면 동등하다")
        void sameReference_isEqual() {
            Skill skill = createSkill("Java", null);

            assertThat(skill).isEqualTo(skill);
        }

        @Test
        @DisplayName("id가 null인 두 인스턴스는 동등하지 않다")
        void nullIdInstances_areNotEqualToEachOther() {
            Skill skill1 = createSkill("Java", null);
            Skill skill2 = createSkill("Java", null);

            assertThat(skill1).isNotEqualTo(skill2);
        }

        @Test
        @DisplayName("같은 id를 가지면 동등하다")
        void sameId_areEqual() {
            Skill skill1 = createSkill("Java", null);
            Skill skill2 = createSkill("Spring", "웹 프레임워크");
            ReflectionTestUtils.setField(skill1, "id", 1L);
            ReflectionTestUtils.setField(skill2, "id", 1L);

            assertThat(skill1).isEqualTo(skill2);
        }

        @Test
        @DisplayName("다른 id를 가지면 동등하지 않다")
        void differentId_areNotEqual() {
            Skill skill1 = createSkill("Java", null);
            Skill skill2 = createSkill("Java", null);
            ReflectionTestUtils.setField(skill1, "id", 1L);
            ReflectionTestUtils.setField(skill2, "id", 2L);

            assertThat(skill1).isNotEqualTo(skill2);
        }

        @Test
        @DisplayName("null과 비교하면 동등하지 않다")
        void notEqualToNull() {
            Skill skill = createSkill("Java", null);

            assertThat(skill).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과 비교하면 동등하지 않다")
        void notEqualToDifferentType() {
            Skill skill = createSkill("Java", null);

            assertThat(skill).isNotEqualTo("Java");
        }

        @Test
        @DisplayName("id가 null이면 hashCode는 0이다")
        void nullIdHashCode_isZero() {
            Skill skill = createSkill("Java", null);

            assertThat(skill.hashCode()).isZero();
        }

        @Test
        @DisplayName("같은 id를 가지면 hashCode가 동일하다")
        void sameId_haveSameHashCode() {
            Skill skill1 = createSkill("Java", null);
            Skill skill2 = createSkill("Spring", "웹 프레임워크");
            ReflectionTestUtils.setField(skill1, "id", 42L);
            ReflectionTestUtils.setField(skill2, "id", 42L);

            assertThat(skill1.hashCode()).isEqualTo(skill2.hashCode());
        }
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
