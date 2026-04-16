package com.generic4.itda.domain.recommendation.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.generic4.itda.domain.recommendation.vo.EmbeddingVector;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmbeddingVectorConverterTest {

    private final EmbeddingVectorConverter converter = new EmbeddingVectorConverter();

    @DisplayName("EmbeddingVector를 JSON 배열 문자열로 직렬화한다")
    @Test
    void convertToDatabaseColumn() {
        // given
        EmbeddingVector embeddingVector = new EmbeddingVector(List.of(0.12, 0.34, 0.56));

        // when
        String json = converter.convertToDatabaseColumn(embeddingVector);

        // then
        assertThat(json).isEqualTo("[0.12,0.34,0.56]");
    }

    @DisplayName("JSON 배열 문자열을 EmbeddingVector로 역직렬화한다")
    @Test
    void convertToEntityAttribute() {
        // given
        String json = "[0.12,0.34,0.56]";

        // when
        EmbeddingVector embeddingVector = converter.convertToEntityAttribute(json);

        // then
        assertThat(embeddingVector).isEqualTo(new EmbeddingVector(List.of(0.12, 0.34, 0.56)));
        assertThat(embeddingVector.dimension()).isEqualTo(3);
        assertThat(embeddingVector.values()).containsExactly(0.12, 0.34, 0.56);
    }

    @DisplayName("직렬화 시 값이 null이면 null을 반환한다")
    @Test
    void convertToDatabaseColumnWithNull() {
        // when
        String json = converter.convertToDatabaseColumn(null);

        // then
        assertThat(json).isNull();
    }

    @DisplayName("역직렬화 시 값이 null이면 null을 반환한다")
    @Test
    void convertToEntityAttributeWithNull() {
        // when
        EmbeddingVector embeddingVector = converter.convertToEntityAttribute(null);

        // then
        assertThat(embeddingVector).isNull();
    }

    @DisplayName("역직렬화 시 값이 공백이면 null을 반환한다")
    @Test
    void convertToEntityAttributeWithBlank() {
        // when
        EmbeddingVector embeddingVector = converter.convertToEntityAttribute("   ");

        // then
        assertThat(embeddingVector).isNull();
    }

    @DisplayName("잘못된 JSON이면 예외를 던진다")
    @Test
    void convertToEntityAttributeWithInvalidJson() {
        // given
        String invalidJson = "{invalid}";

        // when & then
        assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EmbeddingVector");
    }

    @DisplayName("JSON 배열 안에 null 원소가 있으면 예외를 던진다")
    @Test
    void convertToEntityAttributeWithNullElement() {
        // given
        String json = "[0.12,null,0.56]";

        // when & then
        assertThatThrownBy(() -> converter.convertToEntityAttribute(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null 원소");
    }
}