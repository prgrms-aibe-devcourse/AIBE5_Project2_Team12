package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * OpenAiQueryEmbeddingGenerator 실제 API 연동 검증 테스트
 * <p>
 * [실행 전 필수 조건] 1. 환경 변수 또는 설정 파일에 아래 값이 반드시 설정되어야 합니다: - OPENAI_API_KEY=sk-... (OpenAI API 키)
 * <p>
 * 2. 실행 방법 (예시): OPENAI_API_KEY=sk-xxx ./gradlew test --tests "*.OpenAiQueryEmbeddingGeneratorLiveTest"
 * <p>
 * [주의 사항] - 실제 OpenAI API를 호출하므로 API 비용이 발생합니다. - CI/CD 파이프라인에서는 실행되지 않도록 @Disabled가 유지되어야 합니다.
 */
@Disabled("OpenAI 실제 API 호출 테스트 (수동 실행 전용)")
@IntegrationTest
@TestPropertySource(properties = {
        "ai.embedding.enabled=true",
        "ai.embedding.model=text-embedding-3-small",
        "ai.embedding.api-key=${OPENAI_API_KEY:}"
})
@DisplayName("OpenAiQueryEmbeddingGenerator 실제 API 연동 테스트")
class OpenAiQueryEmbeddingGeneratorLiveTest {

    @Autowired
    private OpenAiQueryEmbeddingGenerator generator;

    @Test
    @DisplayName("실제 OpenAI API 호출 시 embedding 벡터를 정상적으로 반환한다")
    void generate_실제API호출_embedding벡터반환() {
        // given
        String queryText = "Java Spring backend developer with PostgreSQL experience";

        // when
        List<Double> embedding = generator.generate(queryText);

        // then
        assertThat(embedding).isNotNull();
        assertThat(embedding).isNotEmpty();
        assertThat(embedding).allSatisfy(value ->
                assertThat(value).isInstanceOf(Double.class)
        );

        System.out.println("embedding size = " + embedding.size());
    }
}
