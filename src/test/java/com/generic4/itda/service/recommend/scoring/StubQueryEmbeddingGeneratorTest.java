package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StubQueryEmbeddingGeneratorTest {

    private final StubQueryEmbeddingGenerator generator = new StubQueryEmbeddingGenerator();

    @Test
    @DisplayName("생성 시 UnsupportedOperationException이 발생한다")
    void 생성_시_UnsupportedOperationException이_발생한다() {
        assertThatThrownBy(() -> generator.generate("any query"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("쿼리 임베딩 생성은 별도 이슈에서 구현합니다.");
    }
}
