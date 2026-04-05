package com.generic4.itda.domain.vo;

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

class PhoneTest {

    @Nested
    @DisplayName("Phone 생성")
    class CreatePhone {

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("com.generic4.itda.domain.vo.PhoneTest#validPhoneSource")
        @DisplayName("정상적인 전화번호이면 정규화되어 저장된다")
        void createPhoneWithValidValue(String input, String expected) {
            // when
            Phone phone = new Phone(input);

            // then
            assertThat(phone.getValue()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("com.generic4.itda.domain.vo.PhoneTest#invalidPhoneSource")
        @DisplayName("유효하지 않은 전화번호이면 예외가 발생한다")
        void throwExceptionWhenInvalidPhone(String input) {
            // when & then
            assertThatThrownBy(() -> new Phone(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Phone 동등성")
    class PhoneEquality {

        @Test
        @DisplayName("포맷이 달라도 같은 번호면 동일하다")
        void equalsAfterNormalization() {
            // given
            Phone phone1 = new Phone("010-1234-5678");
            Phone phone2 = new Phone("01012345678");

            // then
            assertThat(phone1).isEqualTo(phone2);
            assertThat(phone1.hashCode()).isEqualTo(phone2.hashCode());
        }

        @Test
        @DisplayName("값이 다르면 다른 Phone으로 본다")
        void notEqualsWhenDifferentValue() {
            // given
            Phone phone1 = new Phone("010-1234-5678");
            Phone phone2 = new Phone("010-9999-9999");

            // then
            assertThat(phone1).isNotEqualTo(phone2);
        }

        @Test
        @DisplayName("null과는 다르다")
        void notEqualsNull() {
            Phone phone = new Phone("010-1234-5678");

            assertThat(phone).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입과는 다르다")
        void notEqualsOtherType() {
            Phone phone = new Phone("010-1234-5678");

            assertThat(phone).isNotEqualTo("01012345678");
        }
    }

    static Stream<Arguments> validPhoneSource() {
        return Stream.of(
                Arguments.of(Named.of("mobile without hyphen", "01012345678"), "01012345678"),
                Arguments.of(Named.of("mobile with hyphen", "010-1234-5678"), "01012345678"),
                Arguments.of(Named.of("old mobile format", "011-123-4567"), "0111234567"),
                Arguments.of(Named.of("seoul short", "02-123-4567"), "021234567"),
                Arguments.of(Named.of("seoul long", "02-1234-5678"), "0212345678"),
                Arguments.of(Named.of("local number", "031-123-4567"), "0311234567"),
                Arguments.of(Named.of("service number", "1588-1234"), "15881234")
        );
    }

    static Stream<Arguments> invalidPhoneSource() {
        return Stream.of(
                Arguments.of(Named.of("null", null)),
                Arguments.of(Named.of("empty", "")),
                Arguments.of(Named.of("blank", " ")),
                Arguments.of(Named.of("only spaces", "   ")),
                Arguments.of(Named.of("letters included", "010-12ab-5678")),
                Arguments.of(Named.of("wrong hyphen grouping", "010-123-56789")),
                Arguments.of(Named.of("too short", "010-1234-567")),
                Arguments.of(Named.of("too long", "010-12345-5678")),
                Arguments.of(Named.of("invalid prefix", "123-4567-8901")),
                Arguments.of(Named.of("double hyphen", "010--1234-5678")),
                Arguments.of(Named.of("ends with hyphen", "010-1234-5678-")),
                Arguments.of(Named.of("starts with hyphen", "-010-1234-5678")),
                Arguments.of(Named.of("contains spaces", "010 1234 5678")),
                Arguments.of(Named.of("international format", "+82-10-1234-5678")),
                Arguments.of(Named.of("wrong short grouping", "010-12-345678")),
                Arguments.of(Named.of("wrong seoul grouping", "02-12-345678")),
                Arguments.of(Named.of("wrong local grouping", "031-12-34567"))
        );
    }
}