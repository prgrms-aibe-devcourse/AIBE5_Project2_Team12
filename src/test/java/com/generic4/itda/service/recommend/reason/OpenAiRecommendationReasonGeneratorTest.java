package com.generic4.itda.service.recommend.reason;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generic4.itda.config.ai.RecommendationReasonProperties;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class OpenAiRecommendationReasonGeneratorTest {

    private static final String API_KEY = "test-api-key";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpServer server;
    private String apiUrl;
    private AtomicReference<CapturedRequest> capturedRequest;
    private AtomicInteger responseStatus;
    private AtomicReference<String> responseBody;
    private OpenAiRecommendationReasonGenerator generator;

    @BeforeEach
    void setUp() throws IOException {
        capturedRequest = new AtomicReference<>();
        responseStatus = new AtomicInteger(200);
        responseBody = new AtomicReference<>("{}");

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/responses", this::handleExchange);
        server.start();

        apiUrl = "http://127.0.0.1:%d/v1/responses".formatted(server.getAddress().getPort());

        RecommendationReasonProperties properties = new RecommendationReasonProperties();
        properties.setEnabled(true);
        properties.setApiUrl(apiUrl);
        properties.setApiKey(API_KEY);
        properties.setModel("gpt-5-mini");
        properties.setMaxOutputTokens(500);

        generator = new OpenAiRecommendationReasonGenerator(properties, objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("정상 응답이면 OpenAI 요청 본문을 구성하고 추천 이유를 반환한다")
    void generate_정상응답_요청본문검증후추천이유반환() throws JsonProcessingException {
        // given
        String expectedReason = "Java와 Spring Boot 경험이 포지션 요구사항과 직접 맞닿아 있습니다. 관련 하이라이트도 뚜렷해 우선 검토 가치가 높습니다.";
        responseBody.set(objectMapper.writeValueAsString(
                java.util.Map.of("output_text", objectMapper.writeValueAsString(java.util.Map.of("reason", expectedReason)))
        ));

        // when
        String reason = generator.generate(createContext());

        // then
        assertThat(reason).isEqualTo(expectedReason);

        CapturedRequest request = capturedRequest.get();
        assertThat(request).isNotNull();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/v1/responses");
        assertThat(request.authorization()).isEqualTo("Bearer " + API_KEY);
        assertThat(request.contentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);

        JsonNode requestBody = objectMapper.readTree(request.body());
        assertThat(requestBody.path("model").asText()).isEqualTo("gpt-5-mini");
        assertThat(requestBody.path("max_output_tokens").asInt()).isEqualTo(500);
        assertThat(requestBody.has("temperature")).isFalse();
        assertThat(requestBody.path("reasoning").path("effort").asText()).isEqualTo("minimal");
        assertThat(requestBody.path("text").path("verbosity").asText()).isEqualTo("low");
        assertThat(requestBody.path("text").path("format").path("type").asText()).isEqualTo("json_object");
        assertThat(requestBody.path("input")).hasSize(2);

        String systemPrompt = requestBody.path("input").get(0).path("content").get(0).path("text").asText();
        String userPrompt = requestBody.path("input").get(1).path("content").get(0).path("text").asText();

        assertThat(systemPrompt).contains("응답은 반드시 JSON 객체 형식이어야 하며, reason 필드 하나만 포함하세요.");
        assertThat(userPrompt).contains("[제안서 제목]");
        assertThat(userPrompt).contains("이커머스 플랫폼 고도화");
        assertThat(userPrompt).contains("[포지션명]");
        assertThat(userPrompt).contains("Java 백엔드 개발자");
        assertThat(userPrompt).contains("[최종 점수]");
        assertThat(userPrompt).contains("0.85");
        assertThat(userPrompt).contains("Java, Spring Boot");
        assertThat(userPrompt).contains("MSA 운영 경험, 대용량 트래픽 처리");
    }

    @Test
    @DisplayName("output_text가 없으면 output 배열의 content text에서 추천 이유를 추출한다")
    void generate_output배열본문에서추천이유추출() throws JsonProcessingException {
        // given
        String expectedReason = "추천 포지션과 맞는 기술 경험이 확인됩니다. 실무 하이라이트도 포지션의 핵심 요구사항과 맞습니다.";
        responseBody.set("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "text": %s
                        }
                      ]
                    }
                  ]
                }
                """.formatted(objectMapper.writeValueAsString(
                objectMapper.writeValueAsString(java.util.Map.of("reason", expectedReason))
        )));

        // when
        String reason = generator.generate(createContext());

        // then
        assertThat(reason).isEqualTo(expectedReason);
    }

    @Test
    @DisplayName("reason 필드 앞뒤 공백은 trim 처리된다")
    void generate_reason앞뒤공백_trim처리() throws JsonProcessingException {
        // given
        responseBody.set(objectMapper.writeValueAsString(
                java.util.Map.of("output_text", objectMapper.writeValueAsString(
                        java.util.Map.of("reason", "  핵심 기술과 하이라이트가 포지션 요구사항과 맞습니다.  ")
                ))
        ));

        // when
        String reason = generator.generate(createContext());

        // then
        assertThat(reason).isEqualTo("핵심 기술과 하이라이트가 포지션 요구사항과 맞습니다.");
    }

    @Test
    @DisplayName("응답 본문이 비어 있으면 예외가 발생한다")
    void generate_응답본문비어있음_예외발생() {
        // given
        responseBody.set("");

        // when / then
        assertThatThrownBy(() -> generator.generate(createContext()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("추천 이유 생성 응답이 비어있습니다.");
    }

    @Test
    @DisplayName("reason 필드가 비어 있으면 예외가 발생한다")
    void generate_reason필드없음_예외발생() throws JsonProcessingException {
        // given
        responseBody.set(objectMapper.writeValueAsString(
                java.util.Map.of("output_text", objectMapper.writeValueAsString(java.util.Map.of()))
        ));

        // when / then
        assertThatThrownBy(() -> generator.generate(createContext()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reason");
    }

    @Test
    @DisplayName("응답에 output_text와 output 배열이 모두 없으면 예외가 발생한다")
    void generate_텍스트응답없음_예외발생() {
        // given
        responseBody.set("{}");

        // when / then
        assertThatThrownBy(() -> generator.generate(createContext()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("추천 이유 생성 응답에서 텍스트를 찾을 수 없습니다.");
    }

    private void handleExchange(HttpExchange exchange) throws IOException {
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        capturedRequest.set(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION),
                exchange.getRequestHeaders().getFirst(HttpHeaders.CONTENT_TYPE),
                new String(requestBytes, StandardCharsets.UTF_8)
        ));

        byte[] responseBytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(responseStatus.get(), responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private RecommendationReasonContext createContext() {
        return new RecommendationReasonContext(
                "이커머스 플랫폼 고도화",
                "Java 백엔드 개발자",
                new ReasonFacts(
                        List.of("Java", "Spring Boot"),
                        List.of("E-commerce"),
                        5,
                        List.of("MSA 운영 경험", "대용량 트래픽 처리")
                ),
                new BigDecimal("0.8500")
        );
    }

    private record CapturedRequest(
            String method,
            String path,
            String authorization,
            String contentType,
            String body
    ) {
    }
}
