package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.generic4.itda.config.ai.AiEmbeddingProperties;
import com.generic4.itda.exception.QueryEmbeddingGenerationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * OpenAiQueryEmbeddingGenerator 단위 테스트
 *
 * [mocking 전략]
 * RestClient fluent chain 전체를 mock하는 방식은 Spring 6.2 + Mockito 5 조합에서
 * 오버로드 충돌(body/uri vararg)로 불안정하다.
 *
 * 대신 Spring이 공식 제공하는 MockRestServiceServer를 사용한다.
 * - 실제 RestClient.Builder에 바인딩하여 HTTP 레이어만 가로챔
 * - fluent chain 자체는 실제로 동작하므로 chain mocking 문제 없음
 * - 프로덕션 코드와 동일한 직렬화/역직렬화 경로를 검증 가능
 */
@DisplayName("OpenAiQueryEmbeddingGenerator 단위 테스트")
class OpenAiQueryEmbeddingGeneratorTest {

    private static final String API_URL = "https://api.openai.com/v1/embeddings";

    private MockRestServiceServer mockServer;
    private OpenAiQueryEmbeddingGenerator generator;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        AiEmbeddingProperties properties = new AiEmbeddingProperties();
        properties.setApiKey("test-api-key");
        properties.setApiUrl(API_URL);
        properties.setModel("text-embedding-3-small");

        generator = new OpenAiQueryEmbeddingGenerator(builder, properties);
    }

    @Nested
    @DisplayName("generate() - 정상 경로")
    class SuccessTest {

        @Test
        @DisplayName("정상 응답이면 embedding 리스트를 반환한다")
        void generate_정상응답_embeddingList반환() {
            // given
            mockServer.expect(requestTo(API_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {
                              "data": [
                                {
                                  "embedding": [0.12, -0.34, 0.56]
                                }
                              ]
                            }
                            """, MediaType.APPLICATION_JSON));

            // when
            List<Double> result = generator.generate("some text");

            // then
            assertThat(result).containsExactly(0.12, -0.34, 0.56);
            mockServer.verify();
        }

        @Test
        @DisplayName("embedding 값이 정수형이어도 Double로 반환한다")
        void generate_정수형embedding_Double반환() {
            // given
            mockServer.expect(requestTo(API_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            "{\"data\": [{\"embedding\": [1, 2, 3]}]}",
                            MediaType.APPLICATION_JSON
                    ));

            // when
            List<Double> result = generator.generate("query");

            // then
            assertThat(result).containsExactly(1.0, 2.0, 3.0);
        }
    }

    @Nested
    @DisplayName("generate() - 입력 검증")
    class InputValidationTest {

        @Test
        @DisplayName("queryText가 null이면 IllegalArgumentException이 발생한다")
        void generate_nullQueryText_예외발생() {
            assertThatThrownBy(() -> generator.generate(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("queryText가 빈 문자열이면 IllegalArgumentException이 발생한다")
        void generate_emptyQueryText_예외발생() {
            assertThatThrownBy(() -> generator.generate(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("queryText가 blank이면 IllegalArgumentException이 발생한다")
        void generate_blankQueryText_예외발생() {
            assertThatThrownBy(() -> generator.generate("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("generate() - 응답 오류 처리")
    class ResponseErrorTest {

        @Test
        @DisplayName("응답 body가 비어 있으면 QueryEmbeddingGenerationException을 던진다")
        void generate_emptyResponseBody_예외던짐() {
            // given
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

            // when/then
            assertThatThrownBy(() -> generator.generate("some text"))
                    .isInstanceOf(QueryEmbeddingGenerationException.class)
                    .hasMessageContaining("비어있습니다");
        }

        @Test
        @DisplayName("응답에 data 필드가 없으면 QueryEmbeddingGenerationException을 던진다")
        void generate_dataFieldMissing_예외던짐() {
            // given
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

            // when/then
            assertThatThrownBy(() -> generator.generate("some text"))
                    .isInstanceOf(QueryEmbeddingGenerationException.class)
                    .hasMessageContaining("data");
        }

        @Test
        @DisplayName("응답 data가 빈 배열이면 QueryEmbeddingGenerationException을 던진다")
        void generate_dataEmpty_예외던짐() {
            // given
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess("{\"data\": []}", MediaType.APPLICATION_JSON));

            // when/then
            assertThatThrownBy(() -> generator.generate("some text"))
                    .isInstanceOf(QueryEmbeddingGenerationException.class)
                    .hasMessageContaining("data");
        }

        @Test
        @DisplayName("첫 번째 data item에 embedding 필드가 없으면 QueryEmbeddingGenerationException을 던진다")
        void generate_embeddingFieldMissing_예외던짐() {
            // given
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess("{\"data\": [{}]}", MediaType.APPLICATION_JSON));

            // when/then
            assertThatThrownBy(() -> generator.generate("some text"))
                    .isInstanceOf(QueryEmbeddingGenerationException.class)
                    .hasMessageContaining("embedding");
        }

        @Test
        @DisplayName("embedding 배열이 비어 있으면 QueryEmbeddingGenerationException을 던진다")
        void generate_embeddingEmpty_예외던짐() {
            // given
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess(
                            "{\"data\": [{\"embedding\": []}]}",
                            MediaType.APPLICATION_JSON
                    ));

            // when/then
            assertThatThrownBy(() -> generator.generate("some text"))
                    .isInstanceOf(QueryEmbeddingGenerationException.class)
                    .hasMessageContaining("embedding");
        }

        @Test
        @DisplayName("embedding 배열에 숫자가 아닌 값이 있으면 QueryEmbeddingGenerationException을 던진다")
        void generate_nonNumericEmbedding_예외던짐() {
            // given
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withSuccess(
                            "{\"data\": [{\"embedding\": [0.12, \"not-a-number\", 0.56]}]}",
                            MediaType.APPLICATION_JSON
                    ));

            // when/then
            assertThatThrownBy(() -> generator.generate("some text"))
                    .isInstanceOf(QueryEmbeddingGenerationException.class)
                    .hasMessageContaining("형식");
        }
    }

    @Nested
    @DisplayName("generate() - 외부 호출 예외 처리")
    class ExternalCallErrorTest {

        @Test
        @DisplayName("서버 5xx 응답이면 QueryEmbeddingGenerationException으로 감싸서 던진다")
        void generate_서버오류응답_감싸서던짐() {
            // given
            mockServer.expect(requestTo(API_URL))
                    .andRespond(withServerError());

            // when/then
            assertThatThrownBy(() -> generator.generate("some text"))
                    .isInstanceOf(QueryEmbeddingGenerationException.class);
        }
    }
}
