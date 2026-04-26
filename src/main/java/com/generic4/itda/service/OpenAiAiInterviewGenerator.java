package com.generic4.itda.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generic4.itda.config.ai.AiBriefProperties;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.proposal.AiBriefPositionResult;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.proposal.AiBriefSkillResult;
import com.generic4.itda.dto.proposal.AiInterviewGenerateRequest;
import com.generic4.itda.dto.proposal.AiInterviewResult;
import com.generic4.itda.dto.proposal.ProposalForm;
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
public class OpenAiAiInterviewGenerator implements AiInterviewGenerator {

    private static final String INTERVIEW_GENERATION_INSTRUCTIONS = """
            당신은 IT 외주/프로젝트 제안서를 완성하기 위한 AI 인터뷰 도우미다.
            사용자의 현재 제안서 폼 상태, 이전 대화 히스토리, 방금 사용자 메시지를 바탕으로
            제안서 폼을 점진적으로 갱신할 JSON과 다음에 물어볼 후속 질문을 생성한다.

            반드시 지킬 규칙:
            - 반드시 JSON만 반환한다.
            - 마크다운 코드블록, 설명문, 주석, 추가 텍스트를 절대 포함하지 않는다.
            - aiBriefResult에는 현재까지 확인된 정보를 반영한 제안서 초안을 반환한다.
            - assistantMessage에는 사용자에게 보여줄 다음 AI 메시지를 한국어로 자연스럽게 작성한다.
            - AI 인터뷰는 제안서를 새로 생성하는 기능이 아니라 기존 제안서 폼을 부분 수정하는 기능이다.
            - 기존 폼에 이미 있는 값은 사용자가 명시적으로 바꾸거나 삭제하라고 하지 않았다면 유지한다.
            - 근거가 부족한 값은 추측하지 말고 null로 반환한다.
            - null, blank, 빈 배열은 기존 값을 삭제하라는 뜻이 아니다.
            - 사용자가 정정한 값은 최신 사용자 메시지를 우선한다.
            - 사용자가 명시적으로 제거/삭제/빼달라고 한 포지션만 삭제 대상으로 본다.
            - 사용자가 제거/삭제/빼달라고 명시하지 않은 기존 포지션은 aiBriefResult.positions에 없더라도 삭제하지 않는다.
            - 같은 positionCategoryName에 여러 title이 있는데 사용자가 category만 언급하며 제거/삭제/빼달라고 하면 바로 삭제하지 않는다.
            - 이 경우 assistantMessage로 어떤 title의 모집 단위를 삭제할지 되묻고, aiBriefResult.positions에는 기존 모집 단위를 유지한다.
            - 모호한 category 삭제 요청이 있을 때 같은 category의 새 title을 임의로 만들어 기존 모집 단위를 대체하지 않는다.
            - totalBudgetMin, totalBudgetMax, unitBudgetMin, unitBudgetMax는 원화 기준 정수로 반환한다.
            - expectedPeriod는 주 단위 기준 정수로 반환한다.
            - positions는 실제 모집 단위 배열이다.
            - positionCategoryName은 공용 직무 마스터에 대응하는 큰 분류명이다. 예: 백엔드 개발자, 모바일 앱 개발자, QA 엔지니어
            - title은 사용자가 화면에서 보게 될 구체 포지션 제목이다. 예: Java Spring 백엔드 개발자, React 프론트엔드 개발자
            - 같은 positionCategoryName이라도 title이 다르고 역할이 명확히 다르면 별도 position으로 분리할 수 있다.
            - 동일한 title을 불필요하게 중복 생성하지 않는다.
            - 같은 positionCategoryName 안에서 역할 차이가 명확하지 않으면 하나의 모집 단위 title/description에 통합해서 표현한다.
            - 정보가 부족하면 positions는 1~3개 이내로 생성한다.
            - 사용자가 명시하지 않은 포지션은 과도하게 늘리지 않는다.

            기존 모집 단위 갱신 규칙:
            - 기존 모집 단위를 수정할 때 사용자가 언급한 필드만 변경한다.
            - 기존 모집 단위를 수정할 때도 positionCategoryName과 title은 대상 식별을 위해 반드시 현재 폼의 값을 그대로 반환한다.
            - 기존 모집 단위의 title은 식별 키로 사용되므로 AI 인터뷰에서 직접 변경하지 않는다.
            - 사용자가 title 변경을 요청하더라도 title은 기존 값을 그대로 반환한다.
            - title 변경 요청이 있으면 assistantMessage에서 현재는 모집 단위 제목 변경을 직접 반영하지 않고 별도 수정이 필요하다고 짧게 안내한다.
            - 기존 모집 단위의 workType, workPlace, headCount, unitBudgetMin, unitBudgetMax, expectedPeriod, careerMinYears, careerMaxYears는 사용자가 변경 의도를 밝힌 경우에만 값을 반환한다.
            - 기존 모집 단위에서 사용자가 언급하지 않은 수정 필드는 null로 반환한다.
            - 단, positionCategoryName과 title은 기존 모집 단위 식별을 위해 null로 반환하지 않는다.
            - 기존 모집 단위에서 headCount가 명확하지 않다고 해서 1로 채우지 않는다.
            - 기존 모집 단위에서 workType이 명확하지 않다고 해서 REMOTE로 채우지 않는다.
            - 기존 모집 단위에서 workPlace가 명확하지 않다고 해서 "협의"로 채우지 않는다.
            - 기존 모집 단위에서 workType이 REMOTE로 명시 변경된 경우에만 workPlace를 null로 반환한다.
            - 기존 모집 단위에서 workType이 SITE 또는 HYBRID로 명시 변경됐고 workPlace도 명확히 말한 경우에만 workPlace를 반환한다.
            - 기존 모집 단위에서 workType이 SITE 또는 HYBRID로 명시 변경됐지만 근무지를 알 수 없으면 workPlace는 null로 반환한다.
            - 서버는 기존 모집 단위의 null 필드를 기존 값 유지로 처리한다.

            신규 모집 단위 생성 규칙:
            - 신규 모집 단위를 생성할 때는 필수값 보정을 허용한다.
            - 신규 모집 단위의 workType은 SITE, REMOTE, HYBRID 중 하나만 사용한다.
            - 신규 모집 단위에서 workType이 명확하지 않으면 REMOTE로 둔다.
            - 신규 모집 단위에서 REMOTE인 경우 workPlace는 null로 둔다.
            - 신규 모집 단위에서 SITE 또는 HYBRID인데 근무지를 알 수 없으면 workPlace는 "협의"로 둔다.
            - 신규 모집 단위에서 headCount가 명확하지 않으면 1로 둔다.

            스킬 규칙:
            - skills.importance는 ESSENTIAL 또는 PREFERENCE만 사용한다.
            - importance를 확신할 수 없으면 PREFERENCE를 사용한다.
            - position별 skills는 최대 4개까지만 반환한다.
            - skills.skillName은 반드시 아래 정규 Skill 목록 중 하나만 사용한다.
            - 정규 Skill 목록에 없는 스킬은 절대 생성, 제안, 추가, 반환하지 않는다.
            - 사용자가 정규 Skill 목록에 없는 스킬을 요청해도 skills에는 포함하지 않는다.
            - 사용자가 정규 Skill 목록에 없는 스킬을 요청하면 description에 억지로 넣지 않는다.
            - 비슷한 표현은 가장 가까운 정규 Skill 이름으로 변환한다. 예: 리액트, React.js, reactjs는 React로 반환한다.
            - assistantMessage에서도 정규 Skill 목록에 없는 스킬을 먼저 제안하거나 추가해드릴지 묻지 않는다.
            - 사용자가 정규 Skill 목록에 없는 스킬 추가를 요청한 경우, assistantMessage에는 등록된 스킬 목록에 없어 추가하지 않았다고 짧게 안내한다.
            - 기존 모집 단위에서 사용자가 스킬을 언급하지 않았다면 skills는 빈 배열로 반환한다.
            - 기존 모집 단위에서 skills 빈 배열은 기존 스킬 삭제가 아니라 기존 스킬 유지 의미로 처리된다.
            - 기존 모집 단위에서 사용자가 새 스킬 추가를 요청한 경우 추가할 스킬만 skills에 담아 반환한다.
            - 기존 모집 단위에서 사용자가 기존 스킬 중요도 변경을 요청한 경우 해당 스킬만 skills에 담아 반환한다.
            - 기존 모집 단위에서 skills에 포함되지 않은 기존 스킬은 서버에서 삭제하지 않는다.
            - 스킬 개별 삭제, 전체 삭제, 전체 교체는 현재 응답 스키마에서 구조적으로 표현하지 않는다.
            - 사용자가 스킬 삭제나 전체 교체를 요청하면 assistantMessage에서 현재는 삭제/교체 대상 확인이 필요하다고 짧게 안내하고, 기존 스킬을 임의로 제거하지 않는다.

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

            질문 방식:
            - 폼에 비어 있는 필드를 전부 한 번에 나열해서 묻지 않는다.
            - 한 번에 1~2개의 주제만 묻는다.
            - 포지션이 여러 개면 하나의 포지션을 먼저 구체화한 뒤 다음 포지션으로 넘어간다.
            - 예: 백엔드 개발자 → 인원/근무형태 → 기간/예산 → 기술스택/경력 → 프론트엔드 개발자 순서
            - assistantMessage에는 방금 반영한 내용이 있으면 짧게 요약하고, 다음 질문을 이어서 작성한다.
            - 이미 충분히 구체화된 항목은 반복해서 묻지 않는다.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiBriefProperties properties;
    private final PositionResolver positionResolver;

    public OpenAiAiInterviewGenerator(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AiBriefProperties properties,
            PositionResolver positionResolver
    ) {
        Assert.hasText(properties.getApiKey(), "AI 브리프 API 키는 필수값입니다.");

        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.positionResolver = positionResolver;
    }

    @Override
    public AiInterviewResult generate(AiInterviewGenerateRequest request) {
        Assert.notNull(request, "AI 인터뷰 요청은 필수값입니다.");

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
                return toAiInterviewResult(responseText);
            } catch (JsonProcessingException exception) {
                log.warn("AI 인터뷰 응답 JSON 파싱 실패. responseText={}", abbreviate(responseText, 2000), exception);
                throw exception;
            }
        } catch (AiBriefGenerationException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AiBriefGenerationException("OpenAI AI 인터뷰 호출에 실패했습니다.", exception);
        } catch (JsonProcessingException exception) {
            throw new AiBriefGenerationException("AI 인터뷰 응답 파싱에 실패했습니다.", exception);
        } catch (RuntimeException exception) {
            throw new AiBriefGenerationException("AI 인터뷰 생성에 실패했습니다.", exception);
        }
    }

    private Map<String, Object> buildRequestBody(AiInterviewGenerateRequest request) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("instructions", buildInstructions());
        requestBody.put("input", buildInput(request));
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

        return INTERVIEW_GENERATION_INSTRUCTIONS + """

                허용 가능한 Position 카테고리 목록:
                %s

                Position 카테고리 추가 규칙:
                - positionCategoryName은 반드시 허용 가능한 Position 카테고리 목록 중 하나만 사용한다.
                - 목록에 없는 직무 카테고리는 절대 새로 만들거나 비슷하게 지어내지 않는다.
                - 사용자의 표현이 목록과 다르면 가장 가까운 기존 Position 카테고리로만 매핑한다.
                - 어떤 기존 Position 카테고리에도 안전하게 매핑되지 않으면 그 포지션은 positions에 포함하지 않는다.
                """.formatted(positionCategoryGuide);
    }

    private String buildInput(AiInterviewGenerateRequest request) {
        return """
                [현재 제안서 폼 상태]
                %s

                [이전 대화 히스토리]
                %s

                [방금 사용자 메시지]
                %s
                """.formatted(
                toJson(request.getProposalForm()),
                blankToNone(request.getConversationText()),
                request.getUserMessage()
        );
    }

    private String toJson(ProposalForm proposalForm) {
        try {
            return objectMapper.writeValueAsString(proposalForm);
        } catch (JsonProcessingException exception) {
            throw new AiBriefGenerationException("AI 인터뷰 제안서 폼 직렬화에 실패했습니다.", exception);
        }
    }

    private String blankToNone(String value) {
        return StringUtils.hasText(value) ? value : "(없음)";
    }

    private Map<String, Object> buildJsonSchemaFormat() {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "proposal_ai_interview");
        format.put("strict", true);
        format.put("schema", buildSchema());
        return format;
    }

    private Map<String, Object> buildSchema() {
        Map<String, Object> schema = objectSchema();
        schema.put("required", List.of("aiBriefResult", "assistantMessage"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("aiBriefResult", buildAiBriefResultSchema());
        properties.put("assistantMessage", Map.of("type", "string"));

        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> buildAiBriefResultSchema() {
        Map<String, Object> schema = objectSchema();

        schema.put("required", List.of(
                "title",
                "description",
                "totalBudgetMin",
                "totalBudgetMax",
                "expectedPeriod",
                "positions"
        ));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("title", nullableStringSchema());
        properties.put("description", nullableStringSchema());
        properties.put("totalBudgetMin", nullableIntegerSchema());
        properties.put("totalBudgetMax", nullableIntegerSchema());
        properties.put("expectedPeriod", nullableIntegerSchema());
        properties.put("positions", Map.of(
                "type", "array",
                "items", buildPositionSchema()
        ));

        schema.put("properties", properties);
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
            throw new AiBriefGenerationException("AI 인터뷰 응답이 비어 있습니다.");
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
            throw new AiBriefGenerationException("AI 인터뷰 생성을 거부했습니다. " + refusalMessage);
        }
        throw new AiBriefGenerationException("AI 인터뷰 응답에서 본문을 찾을 수 없습니다.");
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

    private AiInterviewResult toAiInterviewResult(String responseText) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(extractJsonObject(responseText));

        return AiInterviewResult.of(
                parseAiBriefResult(root.get("aiBriefResult")),
                normalizeRequiredText(root.get("assistantMessage"), "AI 인터뷰 후속 질문은 필수값입니다.")
        );
    }

    private AiBriefResult parseAiBriefResult(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            throw new AiBriefGenerationException("AI 인터뷰 브리프 결과가 비어 있습니다.");
        }

        return AiBriefResult.of(
                normalizeText(node.get("title")),
                normalizeText(node.get("description")),
                asLong(node.get("totalBudgetMin")),
                asLong(node.get("totalBudgetMax")),
                asLong(node.get("expectedPeriod")),
                parsePositions(node.get("positions"))
        );
    }

    private String extractJsonObject(String responseText) throws JsonProcessingException {
        if (!StringUtils.hasText(responseText)) {
            throw new JsonProcessingException("AI 인터뷰 응답 본문이 비어 있습니다.") {
            };
        }

        String text = stripCodeFence(responseText.trim());
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start < 0 || end < start) {
            throw new JsonProcessingException("AI 인터뷰 응답에서 JSON 객체를 찾을 수 없습니다. responseText="
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
                    normalizeRequiredText(positionNode.get("positionCategoryName"), "AI 인터뷰 포지션 카테고리는 필수값입니다."),
                    normalizeRequiredText(positionNode.get("title"), "AI 인터뷰 포지션 제목은 필수값입니다."),
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
                    normalizeRequiredText(skillNode.get("skillName"), "AI 인터뷰 스킬명은 필수값입니다."),
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