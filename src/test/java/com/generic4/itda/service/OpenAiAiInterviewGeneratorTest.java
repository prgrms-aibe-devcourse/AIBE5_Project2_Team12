package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generic4.itda.config.ai.AiBriefProperties;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.proposal.AiInterviewGenerateRequest;
import com.generic4.itda.dto.proposal.AiInterviewResult;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.exception.AiBriefGenerationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiAiInterviewGeneratorTest {

    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String API_KEY = "test-api-key";
    private static final List<String> ALLOWED_POSITIONS = List.of(
            "백엔드 개발자",
            "프론트엔드 개발자",
            "풀스택 개발자",
            "모바일 앱 개발자",
            "AI 엔지니어",
            "데이터 엔지니어",
            "DevOps 엔지니어",
            "UI/UX 디자이너",
            "서비스 기획자",
            "QA 엔지니어"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer server;

    private OpenAiAiInterviewGenerator generator;

    @BeforeEach
    void setUp() {
        AiBriefProperties properties = new AiBriefProperties();
        properties.setEnabled(true);
        properties.setApiUrl(API_URL);
        properties.setApiKey(API_KEY);
        properties.setModel("gpt-5-mini");
        properties.setMaxOutputTokens(2000);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        PositionResolver positionResolver = Mockito.mock(PositionResolver.class);
        Mockito.when(positionResolver.findAllowedCategoryNames())
                .thenReturn(ALLOWED_POSITIONS);
        generator = new OpenAiAiInterviewGenerator(builder, objectMapper, properties, positionResolver);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    @DisplayName("정상 응답이면 AI 인터뷰 결과를 파싱한다")
    void generate_parsesAiInterviewResult() throws JsonProcessingException {
        String aiBriefJson = objectMapper.writeValueAsString(Map.of(
                "aiBriefResult", Map.of(
                        "title", "AI 인터뷰 프로젝트",
                        "description", "AI 인터뷰로 정리한 프로젝트입니다.",
                        "totalBudgetMin", 5000000,
                        "totalBudgetMax", 8000000,
                        "expectedPeriod", 6,
                        "positions", List.of(
                                Map.ofEntries(
                                        Map.entry("proposalPositionId", 11),
                                        Map.entry("positionCategoryName", "백엔드 개발자"),
                                        Map.entry("title", "Java Spring 백엔드 개발자"),
                                        Map.entry("workType", "REMOTE"),
                                        Map.entry("headCount", 1),
                                        Map.entry("unitBudgetMin", 3000000),
                                        Map.entry("unitBudgetMax", 4000000),
                                        Map.entry("expectedPeriod", 6),
                                        Map.entry("careerMinYears", 3),
                                        Map.entry("careerMaxYears", 6),
                                        Map.entry("workPlace", "협의"),
                                        Map.entry("skills", List.of(
                                                Map.of("skillName", "Java", "importance", "ESSENTIAL"),
                                                Map.of("skillName", "Spring Boot", "importance", "PREFERENCE")
                                        ))
                                )
                        )
                ),
                "assistantMessage", "백엔드 포지션을 반영했습니다. 다음으로 예산 범위를 확인해도 될까요?"
        ));

        server.expect(requestTo(API_URL))
                .andExpect(method(POST))
                .andExpect(header(AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("gpt-5-mini"))
                .andExpect(jsonPath("$.input").value(containsString("[현재 제안서 폼 상태]")))
                .andExpect(jsonPath("$.input").value(containsString("[이전 대화 히스토리]")))
                .andExpect(jsonPath("$.input").value(containsString("이전 대화입니다.")))
                .andExpect(jsonPath("$.input").value(containsString("[방금 사용자 메시지]")))
                .andExpect(jsonPath("$.input").value(containsString("백엔드 개발자가 필요해요.")))
                .andExpect(jsonPath("$.instructions").value(containsString("AI 인터뷰는 제안서를 새로 생성하는 기능이 아니라 기존 제안서 폼을 부분 수정하는 기능이다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("기존 폼에 이미 있는 값은 사용자가 명시적으로 바꾸거나 삭제하라고 하지 않았다면 유지한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("null, blank, 빈 배열은 기존 값을 삭제하라는 뜻이 아니다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("proposalPositionId는 현재 제안서 폼의 positions[].id에 대응하는 기존 모집 단위 식별자다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("기존 모집 단위를 수정하는 경우 현재 제안서 폼의 positions[].id 값을 proposalPositionId로 반드시 그대로 반환한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("신규 모집 단위를 생성하는 경우 proposalPositionId는 null로 반환한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("기존 모집 단위를 수정할 때 proposalPositionId는 대상 식별을 위해 반드시 현재 폼의 positions[].id 값을 그대로 반환한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("기존 모집 단위를 수정할 때도 positionCategoryName과 title은 대상 식별 보조 정보로 현재 폼의 값을 그대로 반환한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("기존 모집 단위의 title은 식별 키로 사용되므로 AI 인터뷰에서 직접 변경하지 않는다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("사용자가 title 변경을 요청하더라도 title은 기존 값을 그대로 반환한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("title 변경 요청이 있으면 assistantMessage에서 현재는 모집 단위 제목 변경을 직접 반영하지 않고 별도 수정이 필요하다고 짧게 안내한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("기존 모집 단위에서 사용자가 언급하지 않은 수정 필드는 null로 반환한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("단, proposalPositionId, positionCategoryName, title은 기존 모집 단위 식별을 위해 null로 반환하지 않는다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("기존 모집 단위에서 skills에 포함되지 않은 기존 스킬은 서버에서 삭제하지 않는다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("assistantMessage에는 사용자에게 보여줄 다음 AI 메시지를 한국어로 자연스럽게 작성한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("skills.skillName은 반드시 아래 정규 Skill 목록 중 하나만 사용한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("정규 Skill 목록에 없는 스킬은 절대 생성, 제안, 추가, 반환하지 않는다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("assistantMessage에서도 정규 Skill 목록에 없는 스킬을 먼저 제안하거나 추가해드릴지 묻지 않는다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("허용 가능한 Position 카테고리 목록:")))
                .andExpect(jsonPath("$.instructions").value(containsString("- 백엔드 개발자")))
                .andExpect(jsonPath("$.instructions").value(containsString("- 프론트엔드 개발자")))
                .andExpect(jsonPath("$.instructions").value(containsString("- 모바일 앱 개발자")))
                .andExpect(jsonPath("$.instructions").value(containsString("- AI 엔지니어")))
                .andExpect(jsonPath("$.instructions").value(containsString("- QA 엔지니어")))
                .andExpect(jsonPath("$.instructions").value(containsString("목록에 없는 직무 카테고리는 절대 새로 만들거나 비슷하게 지어내지 않는다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("- React")))
                .andExpect(jsonPath("$.instructions").value(containsString("- Spring Boot")))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.schema.required[*]").value(containsInAnyOrder(
                        "aiBriefResult",
                        "assistantMessage"
                )))
                .andExpect(jsonPath("$.text.format.schema.properties.aiBriefResult.required[*]").value(containsInAnyOrder(
                        "title",
                        "description",
                        "totalBudgetMin",
                        "totalBudgetMax",
                        "expectedPeriod",
                        "positions"
                )))
                .andExpect(jsonPath("$.text.format.schema.properties.aiBriefResult.properties.positions.items.required[*]")
                        .value(containsInAnyOrder(
                                "proposalPositionId",
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
                        )))
                .andExpect(jsonPath("$.text.format.schema.properties.aiBriefResult.properties.positions.items.properties.proposalPositionId.type[*]")
                        .value(containsInAnyOrder("integer", "null")))
                .andExpect(jsonPath("$.text.format.schema.properties.aiBriefResult.properties.positions.items.properties.positionCategoryName.enum[*]")
                        .value(containsInAnyOrder(ALLOWED_POSITIONS.toArray())))
                .andExpect(jsonPath("$.text.format.schema.properties.aiBriefResult.properties.positions.items.properties.skills.items.required[*]")
                        .value(containsInAnyOrder("skillName", "importance")))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(Map.of("output_text", aiBriefJson)),
                        MediaType.APPLICATION_JSON
                ));

        AiInterviewResult result = generator.generate(AiInterviewGenerateRequest.of(
                new ProposalForm(),
                "이전 대화입니다.",
                "백엔드 개발자가 필요해요."
        ));

        assertThat(result.getAssistantMessage()).isEqualTo("백엔드 포지션을 반영했습니다. 다음으로 예산 범위를 확인해도 될까요?");
        assertThat(result.getAiBriefResult().getTitle()).isEqualTo("AI 인터뷰 프로젝트");
        assertThat(result.getAiBriefResult().getDescription()).isEqualTo("AI 인터뷰로 정리한 프로젝트입니다.");
        assertThat(result.getAiBriefResult().getTotalBudgetMin()).isEqualTo(5_000_000L);
        assertThat(result.getAiBriefResult().getTotalBudgetMax()).isEqualTo(8_000_000L);
        assertThat(result.getAiBriefResult().getExpectedPeriod()).isEqualTo(6L);
        assertThat(result.getAiBriefResult().getPositions()).hasSize(1);
        assertThat(result.getAiBriefResult().getPositions().get(0).getProposalPositionId()).isEqualTo(11L);
        assertThat(result.getAiBriefResult().getPositions().get(0).getPositionCategoryName()).isEqualTo("백엔드 개발자");
        assertThat(result.getAiBriefResult().getPositions().get(0).getTitle()).isEqualTo("Java Spring 백엔드 개발자");
        assertThat(result.getAiBriefResult().getPositions().get(0).getWorkType()).isEqualTo(ProposalWorkType.REMOTE);
        assertThat(result.getAiBriefResult().getPositions().get(0).getExpectedPeriod()).isEqualTo(6L);
        assertThat(result.getAiBriefResult().getPositions().get(0).getCareerMinYears()).isEqualTo(3);
        assertThat(result.getAiBriefResult().getPositions().get(0).getCareerMaxYears()).isEqualTo(6);
        assertThat(result.getAiBriefResult().getPositions().get(0).getWorkPlace()).isEqualTo("협의");
        assertThat(result.getAiBriefResult().getPositions().get(0).getSkills()).hasSize(2);
        assertThat(result.getAiBriefResult().getPositions().get(0).getSkills().get(0).getSkillName()).isEqualTo("Java");
        assertThat(result.getAiBriefResult().getPositions().get(0).getSkills().get(0).getImportance())
                .isEqualTo(ProposalPositionSkillImportance.ESSENTIAL);
    }

    @Test
    @DisplayName("응답 본문이 없으면 AI 인터뷰 생성 예외가 발생한다")
    void failWhenResponseHasNoTextBody() throws JsonProcessingException {
        server.expect(requestTo(API_URL))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(Map.of("output", List.of())),
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> generator.generate(AiInterviewGenerateRequest.of(
                new ProposalForm(),
                "",
                "사용자 메시지"
        )))
                .isInstanceOf(AiBriefGenerationException.class)
                .hasMessage("AI 인터뷰 응답에서 본문을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("지원하지 않는 근무 형태가 오면 파싱에 실패한다")
    void failWhenWorkTypeIsUnsupported() throws JsonProcessingException {
        String aiBriefJson = objectMapper.writeValueAsString(Map.of(
                "aiBriefResult", Map.of(
                        "title", "제목",
                        "description", "설명",
                        "positions", List.of(
                                Map.ofEntries(
                                        Map.entry("proposalPositionId", 1),
                                        Map.entry("positionCategoryName", "백엔드 개발자"),
                                        Map.entry("title", "백엔드 개발자"),
                                        Map.entry("workType", "OFFICE"),
                                        Map.entry("headCount", 1),
                                        Map.entry("unitBudgetMin", 1000000),
                                        Map.entry("unitBudgetMax", 2000000),
                                        Map.entry("expectedPeriod", 4),
                                        Map.entry("careerMinYears", 1),
                                        Map.entry("careerMaxYears", 3),
                                        Map.entry("workPlace", "판교"),
                                        Map.entry("skills", List.of())
                                )
                        )
                ),
                "assistantMessage", "확인했습니다."
        ));

        server.expect(requestTo(API_URL))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(Map.of("output_text", aiBriefJson)),
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> generator.generate(AiInterviewGenerateRequest.of(
                new ProposalForm(),
                "",
                "사용자 메시지"
        )))
                .isInstanceOf(AiBriefGenerationException.class)
                .hasMessage("지원하지 않는 근무 형태입니다. value=OFFICE");
    }

    @Test
    @DisplayName("OpenAI 호출이 실패하면 AI 인터뷰 생성 예외로 감싼다")
    void failWhenOpenAiCallFails() {
        server.expect(requestTo(API_URL))
                .andExpect(method(POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> generator.generate(AiInterviewGenerateRequest.of(
                new ProposalForm(),
                "",
                "사용자 메시지"
        )))
                .isInstanceOf(AiBriefGenerationException.class)
                .hasMessage("OpenAI AI 인터뷰 호출에 실패했습니다.");
    }
}