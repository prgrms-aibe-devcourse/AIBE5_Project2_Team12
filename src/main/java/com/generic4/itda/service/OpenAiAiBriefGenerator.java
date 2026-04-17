package com.generic4.itda.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generic4.itda.config.ai.AiBriefProperties;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.proposal.AiBriefGenerateRequest;
import com.generic4.itda.dto.proposal.AiBriefPositionResult;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.proposal.AiBriefSkillResult;
import com.generic4.itda.exception.AiBriefGenerationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "ai.brief", name = "enabled", havingValue = "true")
public class OpenAiAiBriefGenerator implements AiBriefGenerator {

    private static final String BRIEF_GENERATION_INSTRUCTIONS = """
            당신은 IT 외주/프로젝트 제안서를 구조화하는 도우미다.
            사용자의 자유 입력을 읽고 현재 시스템에서 바로 저장 가능한 제안서 초안 JSON으로 변환한다.

            규칙:
            - 반드시 JSON만 반환한다.
            - title, description은 한국어로 자연스럽게 작성한다.
            - 근거가 부족한 값은 추측하지 말고 필드를 생략한다.
            - workType은 SITE, REMOTE, HYBRID 중 하나만 사용한다.
            - totalBudgetMin, totalBudgetMax, unitBudgetMin, unitBudgetMax는 원화 기준 정수로 반환한다.
            - expectedPeriod는 주 단위 기준 정수로 반환한다.
            - positions는 직무명별로 나누고 같은 직무를 중복 생성하지 않는다.
            - skills.importance는 ESSENTIAL 또는 PREFERENCE만 사용한다.
            - importance를 확신할 수 없으면 PREFERENCE를 사용한다.
            - skillName, positionName은 짧고 일반적인 명칭으로 정리한다.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiBriefProperties properties;

    public OpenAiAiBriefGenerator(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
            AiBriefProperties properties) {
        Assert.hasText(properties.getApiKey(), "AI 브리프 API 키는 필수값입니다.");

        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AiBriefResult generate(AiBriefGenerateRequest request) {
        Assert.notNull(request, "AI 브리프 요청은 필수값입니다.");

        try {
            JsonNode responseBody = restClient.post()
                    .uri(properties.getApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                    .body(buildRequestBody(request))
                    .retrieve()
                    .body(JsonNode.class);

            String responseText = extractResponseText(responseBody);
            return toAiBriefResult(responseText);
        } catch (AiBriefGenerationException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiBriefGenerationException("OpenAI AI 브리프 호출에 실패했습니다.", exception);
        } catch (JsonProcessingException exception) {
            throw new AiBriefGenerationException("AI 브리프 응답 파싱에 실패했습니다.", exception);
        } catch (RuntimeException exception) {
            throw new AiBriefGenerationException("AI 브리프 생성에 실패했습니다.", exception);
        }
    }

    private Map<String, Object> buildRequestBody(AiBriefGenerateRequest request) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("instructions", BRIEF_GENERATION_INSTRUCTIONS);
        requestBody.put("input", request.getRawInputText());
        requestBody.put("max_output_tokens", properties.getMaxOutputTokens());

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("format", buildJsonSchemaFormat());
        requestBody.put("text", text);

        return requestBody;
    }

    private Map<String, Object> buildJsonSchemaFormat() {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "proposal_ai_brief");
        format.put("strict", true);
        format.put("schema", buildSchema());
        return format;
    }

    private Map<String, Object> buildSchema() {
        Map<String, Object> schema = objectSchema();

        schema.put("required", List.of(
                "title",
                "description",
                "totalBudgetMin",
                "totalBudgetMax",
                "workType",
                "workPlace",
                "expectedPeriod",
                "positions"
        ));
        schema.put("properties", Map.of(
                "title", nullableStringSchema(),
                "description", nullableStringSchema(),
                "totalBudgetMin", nullableIntegerSchema(),
                "totalBudgetMax", nullableIntegerSchema(),
                "workType", nullableEnumSchema("SITE", "REMOTE", "HYBRID"),
                "workPlace", nullableStringSchema(),
                "expectedPeriod", nullableIntegerSchema(),
                "positions", Map.of(
                        "type", "array",
                        "items", buildPositionSchema()
                )
        ));

        return schema;
    }

    private Map<String, Object> buildPositionSchema() {
        Map<String, Object> schema = objectSchema();
        schema.put("required", List.of("positionName", "headCount", "unitBudgetMin", "unitBudgetMax", "skills"));
        schema.put("properties", Map.of(
                "positionName", Map.of("type", "string"),
                "headCount", nullableIntegerSchema(),
                "unitBudgetMin", nullableIntegerSchema(),
                "unitBudgetMax", nullableIntegerSchema(),
                "skills", Map.of(
                        "type", "array",
                        "items", buildSkillSchema()
                )
        ));
        return schema;
    }

    private Map<String, Object> buildSkillSchema() {
        Map<String, Object> schema = objectSchema();
        schema.put("required", List.of("skillName", "importance"));
        schema.put("properties", Map.of(
                "skillName", Map.of("type", "string"),
                "importance", nullableEnumSchema("ESSENTIAL", "PREFERENCE")
        ));
        return schema;
    }

    private Map<String, Object> objectSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        return schema;
    }

    private Map<String, Object> nullableStringSchema() {
        return Map.of("type", List.of("string", "null"));
    }

    private Map<String, Object> nullableIntegerSchema() {
        return Map.of("type", List.of("integer", "null"));
    }

    private Map<String, Object> nullableEnumSchema(String... values) {
        List<Object> enumValues = new ArrayList<>(List.of(values));
        enumValues.add(null);
        return Map.of(
                "type", List.of("string", "null"),
                "enum", enumValues
        );
    }

    private String extractResponseText(JsonNode responseBody) {
        if (responseBody == null || responseBody.isNull()) {
            throw new AiBriefGenerationException("AI 브리프 응답이 비어 있습니다.");
        }

        String directOutput = readText(responseBody.get("output_text"));
        if (StringUtils.hasText(directOutput)) {
            return directOutput;
        }

        String refusalMessage = null;
        JsonNode output = responseBody.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.get("content");
                if (content == null || !content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    String text = readText(contentItem.get("text"));
                    if (StringUtils.hasText(text)) {
                        return text;
                    }
                    String refusal = readText(contentItem.get("refusal"));
                    if (StringUtils.hasText(refusal)) {
                        refusalMessage = refusal;
                    }
                }
            }
        }

        if (StringUtils.hasText(refusalMessage)) {
            throw new AiBriefGenerationException("AI 브리프 생성을 거부했습니다. " + refusalMessage);
        }
        throw new AiBriefGenerationException("AI 브리프 응답에서 본문을 찾을 수 없습니다.");
    }

    private String readText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        JsonNode valueNode = node.get("value");
        if (valueNode != null && valueNode.isTextual()) {
            return valueNode.asText();
        }
        return null;
    }

    private AiBriefResult toAiBriefResult(String responseText) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseText);

        return AiBriefResult.of(
                normalizeText(root.get("title")),
                normalizeText(root.get("description")),
                asLong(root.get("totalBudgetMin")),
                asLong(root.get("totalBudgetMax")),
                parseWorkType(root.get("workType")),
                normalizeText(root.get("workPlace")),
                asLong(root.get("expectedPeriod")),
                parsePositions(root.get("positions"))
        );
    }

    private List<AiBriefPositionResult> parsePositions(JsonNode positionsNode) {
        if (positionsNode == null || positionsNode.isNull() || !positionsNode.isArray()) {
            return List.of();
        }

        List<AiBriefPositionResult> positions = new ArrayList<>();
        for (JsonNode positionNode : positionsNode) {
            positions.add(AiBriefPositionResult.of(
                    normalizeRequiredText(positionNode.get("positionName"), "AI 브리프 직무명은 필수값입니다."),
                    asLong(positionNode.get("headCount")),
                    asLong(positionNode.get("unitBudgetMin")),
                    asLong(positionNode.get("unitBudgetMax")),
                    parseSkills(positionNode.get("skills"))
            ));
        }
        return positions;
    }

    private List<AiBriefSkillResult> parseSkills(JsonNode skillsNode) {
        if (skillsNode == null || skillsNode.isNull() || !skillsNode.isArray()) {
            return List.of();
        }

        List<AiBriefSkillResult> skills = new ArrayList<>();
        for (JsonNode skillNode : skillsNode) {
            skills.add(AiBriefSkillResult.of(
                    normalizeRequiredText(skillNode.get("skillName"), "AI 브리프 스킬명은 필수값입니다."),
                    parseImportance(skillNode.get("importance"))
            ));
        }
        return skills;
    }

    private ProposalWorkType parseWorkType(JsonNode node) {
        String value = normalizeText(node);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ProposalWorkType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AiBriefGenerationException("지원하지 않는 근무 형태입니다. value=" + value, exception);
        }
    }

    private ProposalPositionSkillImportance parseImportance(JsonNode node) {
        String value = normalizeText(node);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ProposalPositionSkillImportance.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AiBriefGenerationException("지원하지 않는 스킬 중요도입니다. value=" + value, exception);
        }
    }

    private Long asLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        String value = normalizeText(node);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new AiBriefGenerationException("숫자 필드 파싱에 실패했습니다. value=" + value, exception);
        }
    }

    private String normalizeRequiredText(JsonNode node, String message) {
        String value = normalizeText(node);
        Assert.hasText(value, message);
        return value;
    }

    private String normalizeText(JsonNode node) {
        String value = readText(node);
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
