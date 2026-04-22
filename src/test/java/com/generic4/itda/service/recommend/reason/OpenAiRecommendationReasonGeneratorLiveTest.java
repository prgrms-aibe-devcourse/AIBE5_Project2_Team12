package com.generic4.itda.service.recommend.reason;

import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.IntegrationTest;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * OpenAiRecommendationReasonGenerator 실제 API 연동 검증 테스트
 * <p>
 * [실행 전 필수 조건] 1. 환경 변수 또는 설정 파일에 아래 값이 반드시 설정되어야 합니다: - OPENAI_API_KEY=sk-... (OpenAI API 키)
 * <p>
 * 2. 실행 방법 (예시): OPENAI_API_KEY=sk-xxx ./gradlew test --tests "*.OpenAiRecommendationReasonGeneratorLiveTest"
 * <p>
 * [주의 사항] - 실제 OpenAI API를 호출하므로 API 비용이 발생합니다. - CI/CD 파이프라인에서는 실행되지 않도록 @Disabled가 유지되어야 합니다.
 */
@Disabled("OpenAI 실제 API 호출 테스트 (수동 실행 전용)")
@IntegrationTest
@TestPropertySource(properties = {
        "ai.recommend-reason.enabled=true",
        "ai.recommend-reason.model=gpt-5-mini",
        "ai.recommend-reason.api-key=${OPENAI_API_KEY:}"
})
@DisplayName("OpenAiRecommendationReasonGenerator 실제 API 연동 테스트")
class OpenAiRecommendationReasonGeneratorLiveTest {

    @Autowired
    private OpenAiRecommendationReasonGenerator generator;

    @Test
    @DisplayName("실제 OpenAI API 호출 시 추천 이유를 정상적으로 반환한다")
    void generate_실제API호출_추천이유반환() {
        // given
        RecommendationReasonContext context = new RecommendationReasonContext(
                "이커머스 플랫폼 고도화",
                "Java 백엔드 개발자",
                new ReasonFacts(
                        List.of("Java", "Spring Boot", "PostgreSQL"),
                        List.of("E-commerce"),
                        5,
                        List.of("MSA 운영 경험", "대용량 트래픽 처리", "결제 시스템 유지보수")
                ),
                new BigDecimal("0.9200")
        );

        // when
        String reason = generator.generate(context);

        // then
        assertThat(reason).isNotBlank();
        System.out.println("recommendation reason = " + reason);
    }
}
