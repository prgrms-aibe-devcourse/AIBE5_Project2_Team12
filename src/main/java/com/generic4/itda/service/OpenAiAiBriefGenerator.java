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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.brief", name = "enabled", havingValue = "true")
public class OpenAiAiBriefGenerator implements AiBriefGenerator {

    private static final String BRIEF_GENERATION_INSTRUCTIONS = """
            당신은 IT 외주/프로젝트 제안서를 구조화하는 도우미다.
            사용자의 자유 입력을 읽고 현재 시스템에서 바로 저장 가능한 제안서 초안 JSON으로 변환한다.

            반드시 지킬 규칙:
            - 반드시 JSON만 반환한다.
            - 마크다운 코드블록, 설명문, 주석, 추가 텍스트를 절대 포함하지 않는다.
            - title, description은 한국어로 자연스럽게 작성한다.
            - aiBriefResult가 아니라 최상위 JSON에 제안서 초안을 반환한다.
            - 근거가 부족한 값은 추측하지 말고 null로 반환한다.
            - 사용자가 정정한 값은 최신 요청을 우선한다.
            - 사용자가 제거/삭제/빼달라고 한 포지션은 positions에서 제외한다.
            - totalBudgetMin, totalBudgetMax, unitBudgetMin, unitBudgetMax는 원화 기준 정수로 반환한다.
            - expectedPeriod는 주 단위 기준 정수로 반환한다.
            - positions는 실제 모집 단위 배열이다.
            - position별 workType은 SITE, REMOTE, HYBRID 중 하나만 사용한다.
            - REMOTE인 경우 workPlace는 null로 둔다.
            - SITE 또는 HYBRID인데 근무지를 알 수 없으면 workPlace는 "협의"로 둔다.
            - headCount가 명확하지 않으면 1로 둔다.
            - positionCategoryName은 공용 직무 마스터에 대응하는 큰 분류명이다. 예: 백엔드 개발자, 모바일 앱 개발자, QA 엔지니어
            - title은 사용자가 화면에서 보게 될 구체 포지션 제목이다. 예: Java Spring 백엔드 개발자, React 프론트엔드 개발자
            - 같은 positionCategoryName이라도 title이 다르고 역할이 명확히 다르면 별도 position으로 분리할 수 있다.
            - 동일한 title을 불필요하게 중복 생성하지 않는다.
            - 같은 positionCategoryName 안에서 역할 차이가 명확하지 않으면 하나의 모집 단위 title/description에 통합해서 표현한다.
            - 정보가 부족하면 positions는 1~3개 이내로 생성한다.
            - 사용자가 명시하지 않은 포지션은 과도하게 늘리지 않는다.
            - 입력은 시간순 요청 목록이다.

            스킬 규칙:
            - skills.importance는 ESSENTIAL 또는 PREFERENCE만 사용한다.
            - importance를 확신할 수 없으면 PREFERENCE를 사용한다.
            - position별 skills는 최대 4개까지만 반환한다.
            - skills.skillName은 반드시 아래 정규 Skill 목록 중 하나만 사용한다.
            - 정규 Skill 목록에 없는 스킬은 절대 생성, 제안, 추가, 반환하지 않는다.
            - 사용자가 정규 Skill 목록에 없는 스킬을 요청해도 skills에는 포함하지 않는다.
            - 사용자가 정규 Skill 목록에 없는 스킬을 요청하면 description에 억지로 넣지 않는다.
            - 비슷한 표현은 가장 가까운 정규 Skill 이름으로 변환한다. 예: 리액트, React.js, reactjs는 React로 반환한다.
            - assistantMessage가 있는 응답 형식이 아니므로 목록 밖 스킬을 안내 문장으로 제안하지 않는다.

            정규 Skill 목록:
            - React
            - Vue
            - Angular
            - Next.js
            - TypeScript
            - JavaScript
            - HTML
            - CSS
            - Tailwind CSS
            - Java
            - Spring
            - Spring Boot
            - Node.js
            - Express
            - NestJS
            - Python
            - Django
            - FastAPI
            - JPA
            - Querydsl
            - REST API
            - GraphQL
            - MySQL
            - PostgreSQL
            - MongoDB
            - Redis
            - Oracle
            - MsSQL
            - Elasticsearch
            - AWS
            - Docker
            - Kubernetes
            - GitHub Actions
            - Nginx
            - Git
            - CI/CD
            - Kafka
            - Jenkins
            - GCP
            - Azure
            - Linux
            - Flutter
            - React Native
            - Swift
            - Kotlin
            - PyTorch
            - TensorFlow
            - LangChain
            - LLM
            - Figma
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiBriefProperties properties;
    private final PositionResolver positionResolver;

    public OpenAiAiBriefGenerator(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
                                  AiBriefProperties properties, PositionResolver positionResolver) {
        Assert.hasText(properties.getApiKey(), "AI 브리프 API 키는 필수값입니다.");

        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.positionResolver = positionResolver;
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
            try {
                return toAiBriefResult(responseText);
            } catch (JsonProcessingException exception) {
                log.warn("AI 브리프 응답 JSON 파싱 실패. responseText={}", abbreviate(responseText, 2000), exception);
                throw exception;
            }
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
        requestBody.put("instructions", buildInstructions());
        requestBody.put("input", request.getRawInputText());
        requestBody.put("max_output_tokens", properties.getMaxOutputTokens());

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("format", buildJsonSchemaFormat());
        requestBody.put("text", text);

        return requestBody;
    }

    private String buildInstructions() {
        String positionCategoryGuide = positionResolver.findAllowedCategoryNames().stream()
                .map(name -> "- " + name)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");

        return BRIEF_GENERATION_INSTRUCTIONS + """

                허용 가능한 Position 카테고리 목록:
                %s

                Position 카테고리 추가 규칙:
                - positionCategoryName은 반드시 허용 가능한 Position 카테고리 목록 중 하나만 사용한다.
                - 목록에 없는 직무 카테고리는 절대 새로 만들거나 비슷하게 지어내지 않는다.
                - 사용자의 표현이 목록과 다르면 가장 가까운 기존 Position 카테고리로만 매핑한다.
                - 어떤 기존 Position 카테고리에도 안전하게 매핑되지 않으면 그 포지션은 positions에 포함하지 않는다.
                """.formatted(positionCategoryGuide);
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
                "expectedPeriod",
                "positions"
        ));
        schema.put("properties", Map.of(
                "title", nullableStringSchema(),
                "description", nullableStringSchema(),
                "totalBudgetMin", nullableIntegerSchema(),
                "totalBudgetMax", nullableIntegerSchema(),
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
        List<String> allowedCategoryNames = positionResolver.findAllowedCategoryNames();
        schema.put("required", List.of(
                "positionCategoryName",
                "title",
                "workType",
                "headCount",
                "unitBudgetMin",
                "unitBudgetMax",
                "expectedPeriod",
                "careerMinYears",
                "careerMaxYears",
                "workPlace",
                "skills"
        ));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("positionCategoryName", enumSchema(allowedCategoryNames));
        properties.put("title", Map.of("type", "string"));
        properties.put("workType", nullableEnumSchema("SITE", "REMOTE", "HYBRID"));
        properties.put("headCount", nullableIntegerSchema());
        properties.put("unitBudgetMin", nullableIntegerSchema());
        properties.put("unitBudgetMax", nullableIntegerSchema());
        properties.put("expectedPeriod", nullableIntegerSchema());
        properties.put("careerMinYears", nullableIntegerSchema());
        properties.put("careerMaxYears", nullableIntegerSchema());
        properties.put("workPlace", nullableStringSchema());
        properties.put("skills", Map.of(
                "type", "array",
                "items", buildSkillSchema()
        ));
        schema.put("properties", properties);
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

    private Map<String, Object> enumSchema(List<String> values) {
        return Map.of(
                "type", "string",
                "enum", values
        );
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
        JsonNode root = objectMapper.readTree(extractJsonObject(responseText));

        return AiBriefResult.of(
                normalizeText(root.get("title")),
                normalizeText(root.get("description")),
                asLong(root.get("totalBudgetMin")),
                asLong(root.get("totalBudgetMax")),
                asLong(root.get("expectedPeriod")),
                parsePositions(root.get("positions"))
        );
    }

    private String extractJsonObject(String responseText) throws JsonProcessingException {
        if (!StringUtils.hasText(responseText)) {
            throw new JsonProcessingException("AI 브리프 응답 본문이 비어 있습니다.") {
            };
        }

        String text = stripCodeFence(responseText.trim());
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start < 0 || end < start) {
            throw new JsonProcessingException("AI 브리프 응답에서 JSON 객체를 찾을 수 없습니다. responseText="
                    + abbreviate(text, 500)) {
            };
        }

        return text.substring(start, end + 1);
    }

    private String stripCodeFence(String responseText) {
        String text = responseText.trim();

        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFenceStart = text.lastIndexOf("```");

            if (firstLineEnd >= 0 && lastFenceStart > firstLineEnd) {
                return text.substring(firstLineEnd + 1, lastFenceStart).trim();
            }
        }

        return text;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private List<AiBriefPositionResult> parsePositions(JsonNode positionsNode) {
        if (positionsNode == null || positionsNode.isNull() || !positionsNode.isArray()) {
            return List.of();
        }

        List<AiBriefPositionResult> positions = new ArrayList<>();
        for (JsonNode positionNode : positionsNode) {
            positions.add(AiBriefPositionResult.of(
                    normalizeRequiredText(positionNode.get("positionCategoryName"), "AI 브리프 포지션 카테고리는 필수값입니다."),
                    normalizeRequiredText(positionNode.get("title"), "AI 브리프 포지션 제목은 필수값입니다."),
                    parseWorkType(positionNode.get("workType")),
                    asLong(positionNode.get("headCount")),
                    asLong(positionNode.get("unitBudgetMin")),
                    asLong(positionNode.get("unitBudgetMax")),
                    asLong(positionNode.get("expectedPeriod")),
                    asInteger(positionNode.get("careerMinYears")),
                    asInteger(positionNode.get("careerMaxYears")),
                    normalizeText(positionNode.get("workPlace")),
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

    private Integer asInteger(JsonNode node) {
        Long value = asLong(node);
        if (value == null) {
            return null;
        }
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new AiBriefGenerationException("정수 범위를 벗어났습니다. value=" + value);
        }
        return value.intValue();
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