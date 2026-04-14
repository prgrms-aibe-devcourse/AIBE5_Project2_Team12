package com.generic4.itda.domain.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PositionTest {

    @DisplayName("유효한 이름이 주어지면 포지션을 생성한다")
    @Test
    void createWithValidName() {
        Position position = Position.create("백엔드 개발자");

        assertThat(position.getName()).isEqualTo("백엔드 개발자");
    }

    @DisplayName("이름에 앞뒤 공백이 있으면 제거하여 저장한다")
    @Test
    void createTrimsWhitespace() {
        Position position = Position.create("  프론트엔드 개발자  ");

        assertThat(position.getName()).isEqualTo("프론트엔드 개발자");
    }

    @DisplayName("포지션명이 누락되면 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenNameIsMissing(String name) {
        assertThatThrownBy(() -> Position.create(name))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포지션명은 필수값입니다.");
    }
}
