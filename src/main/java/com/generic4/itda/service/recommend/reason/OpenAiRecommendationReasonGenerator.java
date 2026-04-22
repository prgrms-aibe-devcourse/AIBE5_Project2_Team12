package com.generic4.itda.service.recommend.reason;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generic4.itda.config.ai.RecommendationReasonProperties;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.recommend-reason", name = "enabled", havingValue = "true")
public class OpenAiRecommendationReasonGenerator implements RecommendationReasonGenerator {

    private final RecommendationReasonProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String generate(RecommendationReasonContext context) {
        Assert.notNull(context, "추천 이유 생성 컨텍스트는 필수입니다.");
        validateProperties();

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> requestBody = createRequestBody(context);

        String responseBody = restClient.post()
                .uri("")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalStateException("추천 이유 생성 응답이 비어있습니다.");
        }

        return extractReason(responseBody);
    }

    private void validateProperties() {
        Assert.hasText(properties.getApiUrl(), "추천 이유 API URL은 필수입니다.");
        Assert.hasText(properties.getApiKey(), "추천 이유 API Key는 필수입니다.");
        Assert.hasText(properties.getModel(), "추천 이유 모델명은 필수입니다.");
        Assert.notNull(properties.getMaxOutputTokens(), "추천 이유 maxOutputTokens는 필수입니다.");
    }

    private Map<String, Object> createRequestBody(RecommendationReasonContext context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("max_output_tokens", properties.getMaxOutputTokens());

        body.put("reasoning", Map.of(
                "effort", "minimal"
        ));

        body.put("text", Map.of(
                "verbosity", "low",
                "format", Map.of(
                        "type", "json_object"
                )
        ));

        body.put("input", List.of(
                Map.of(
                        "role", "system",
                        "content", List.of(
                                Map.of(
                                        "type", "input_text",
                                        "text", createSystemPrompt()
                                )
                        )
                ),
                Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "input_text",
                                        "text", createUserPrompt(context)
                                )
                        )
                )
        ));

        return body;
    }

    private String createSystemPrompt() {
        return """
                당신은 추천 결과 설명 생성기입니다.
                반드시 입력으로 주어진 정보만 사용하세요.
                없는 사실을 추론하거나 과장하지 마세요.
                출력은 한국어 2~3문장 이내의 추천 설명이어야 합니다.
                응답은 반드시 JSON 객체 형식이어야 하며, reason 필드 하나만 포함하세요.
                """;
    }

    private String createUserPrompt(RecommendationReasonContext context) {
        return """
                아래 추천 결과 정보를 바탕으로, 왜 이 후보가 해당 포지션에 적합한지 한국어 2~3문장으로 설명하세요.
                
                [제안서 제목]
                %s
                
                [포지션명]
                %s
                
                [최종 점수]
                %s
                
                [매칭 스킬]
                %s
                
                [경력 연차]
                %d
                
                [하이라이트]
                %s
                """.formatted(
                context.proposalTitle(),
                context.positionName(),
                normalizeScore(context.finalScore()),
                joinList(context.reasonFacts().matchedSkills()),
                context.reasonFacts().careerYears(),
                joinList(context.reasonFacts().highlights())
        );
    }

    private String extractReason(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode outputTextNode = root.path("output_text");
            if (outputTextNode.isTextual() && StringUtils.hasText(outputTextNode.asText())) {
                return extractReasonFromJsonText(outputTextNode.asText());
            }

            JsonNode outputArray = root.path("output");
            if (outputArray.isArray()) {
                for (JsonNode outputItem : outputArray) {
                    JsonNode contentArray = outputItem.path("content");
                    if (!contentArray.isArray()) {
                        continue;
                    }

                    for (JsonNode contentItem : contentArray) {
                        JsonNode textNode = contentItem.path("text");
                        if (textNode.isTextual() && StringUtils.hasText(textNode.asText())) {
                            return extractReasonFromJsonText(textNode.asText());
                        }
                    }
                }
            }

            throw new IllegalStateException("추천 이유 생성 응답에서 텍스트를 찾을 수 없습니다.");
        } catch (IOException e) {
            throw new IllegalStateException("추천 이유 생성 응답 파싱에 실패했습니다.", e);
        }
    }

    private String extractReasonFromJsonText(String jsonText) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonText);
            String reason = jsonNode.path("reason").asText(null);

            if (!StringUtils.hasText(reason)) {
                throw new IllegalStateException("추천 이유(reason) 필드가 비어 있습니다.");
            }

            return reason.trim();
        } catch (IOException e) {
            throw new IllegalStateException("추천 이유 JSON 파싱에 실패했습니다.", e);
        }
    }

    private String normalizeScore(BigDecimal score) {
        return score == null ? "-" : score.stripTrailingZeros().toPlainString();
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        return String.join(", ", values);
    }
}
