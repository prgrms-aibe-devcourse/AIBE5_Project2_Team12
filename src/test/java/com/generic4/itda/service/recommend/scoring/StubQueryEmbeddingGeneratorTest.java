package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StubQueryEmbeddingGeneratorTest {

    private final StubQueryEmbeddingGenerator generator = new StubQueryEmbeddingGenerator();

    @Test
    @DisplayName("같은 입력이면 항상 같은 임베딩을 생성한다")
    void 같은_입력이면_항상_같은_임베딩을_생성한다() {
        List<Double> first = generator.generate("any query");
        List<Double> second = generator.generate("any query");

        double norm = Math.sqrt(first.stream()
                .mapToDouble(value -> value * value)
                .sum());

        assertThat(first).containsExactlyElementsOf(second);
        assertThat(first).hasSize(64);
        assertThat(norm).isCloseTo(1.0d, offset(1e-9));
    }

    @Test
    @DisplayName("입력이 다르면 다른 임베딩을 생성한다")
    void 입력이_다르면_다른_임베딩을_생성한다() {
        assertThat(generator.generate("query A"))
                .isNotEqualTo(generator.generate("query B"));
    }

    @Test
    @DisplayName("빈 입력이면 예외가 발생한다")
    void 빈_입력이면_예외가_발생한다() {
        assertThatThrownBy(() -> generator.generate("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("queryText는 비어있을 수 없습니다.");
    }
}
