package com.generic4.itda.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EmailTest {

    @Nested
    @DisplayName("Email 생성")
    class CreateEmail {

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("com.generic4.itda.domain.member.EmailTest#validEmailSource")
        @DisplayName("정상적인 이메일이면 Email 객체를 생성한다")
        void createEmailWithValidValue(String input) {
            // when
            Email email = new Email(input);

            // then
            assertThat(email.getValue()).isEqualTo(input);
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("com.generic4.itda.domain.member.EmailTest#invalidEmailSource")
        @DisplayName("유효하지 않은 이메일이면 예외가 발생한다")
        void throwExceptionWhenInvalidEmail(String input) {
            // when & then
            assertThatThrownBy(() -> new Email(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Email 동등성")
    class EmailEquality {

        @Test
        @DisplayName("값이 같으면 같은 Email로 본다")
        void equalsWhenSameValue() {
            // given
            Email email1 = new Email("test@example.com");
            Email email2 = new Email("test@example.com");

            // then
            assertThat(email1).isEqualTo(email2);
            assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        }

        @Test
        @DisplayName("값이 다르면 다른 Email로 본다")
        void notEqualsWhenDifferentValue() {
            // given
            Email email1 = new Email("test1@example.com");
            Email email2 = new Email("test2@example.com");

            // then
            assertThat(email1).isNotEqualTo(email2);
        }
    }

    static Stream<String> validEmailSource() {
        return Stream.of(
                "test@example.com",
                "user123@gmail.com",
                "hello.world@naver.com",
                "dev-team@company.co.kr",
                "a@b.com",
                "test+tag@example.com"
        );
    }

    static Stream<Arguments> invalidEmailSource() {
        return Stream.of(
                Arguments.of(Named.of("null", null)),
                Arguments.of(Named.of("empty", "")),
                Arguments.of(Named.of("blank", " ")),
                Arguments.of(Named.of("only spaces", "   ")),
                Arguments.of(Named.of("missing @", "plainaddress")),
                Arguments.of(Named.of("missing local part", "@example.com")),
                Arguments.of(Named.of("missing domain", "test@")),
                Arguments.of(Named.of("missing @ with domain-like text", "test.com")),
                Arguments.of(Named.of("double @", "test@@example.com")),
                Arguments.of(Named.of("space in domain", "test@ example.com")),
                Arguments.of(Named.of("missing top-level domain", "test@example")),
                Arguments.of(Named.of("starts with dot in local part", ".test@example.com")),
                Arguments.of(Named.of("ends with dot in local part", "test.@example.com")),
                Arguments.of(Named.of("double dot in local part", "test..email@example.com")),
                Arguments.of(Named.of("double dot in domain", "test@example..com")),
                Arguments.of(Named.of("domain starts with hyphen", "test@-example.com")),
                Arguments.of(Named.of("domain ends with hyphen", "test@example-.com")),
                Arguments.of(Named.of("missing domain label", "test@.com"))
        );
    }
}
