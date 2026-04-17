package com.generic4.itda.service;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.generic4.itda.dto.proposal.AiBriefGenerateRequest;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.exception.AiBriefGenerationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiAiBriefGeneratorTest {

    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String API_KEY = "test-api-key";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockRestServiceServer server;

    private OpenAiAiBriefGenerator generator;

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
        generator = new OpenAiAiBriefGenerator(builder, objectMapper, properties);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    @DisplayName("정상 응답이면 AI 브리프 결과를 Proposal 저장 구조로 파싱한다")
    void generate_parsesAiBriefResult() throws JsonProcessingException {
        String briefJson = objectMapper.writeValueAsString(Map.of(
                "title", "AI가 정리한 프로젝트 제목",
                "description", "백엔드와 프론트엔드 개발자를 찾는 프로젝트입니다.",
                "totalBudgetMin", 5000000,
                "totalBudgetMax", 8000000,
                "workType", "HYBRID",
                "workPlace", "판교",
                "expectedPeriod", 6,
                "positions", List.of(
                        Map.of(
                                "positionName", "백엔드 개발자",
                                "headCount", 1,
                                "unitBudgetMin", 3000000,
                                "unitBudgetMax", 4000000,
                                "skills", List.of(
                                        Map.of("skillName", "Java", "importance", "ESSENTIAL"),
                                        Map.of("skillName", "Spring Boot", "importance", "PREFERENCE")
                                )
                        )
                )
        ));

        server.expect(requestTo(API_URL))
                .andExpect(method(POST))
                .andExpect(header(AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("gpt-5-mini"))
                .andExpect(jsonPath("$.input").value("프로젝트 원본 입력"))
                .andExpect(jsonPath("$.instructions").value(containsString("근거가 부족한 값은 추측하지 말고 null로 반환한다.")))
                .andExpect(jsonPath("$.instructions").value(containsString("expectedPeriod는 주 단위 기준 정수로 반환한다.")))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.schema.required[*]").value(containsInAnyOrder(
                        "title",
                        "description",
                        "totalBudgetMin",
                        "totalBudgetMax",
                        "workType",
                        "workPlace",
                        "expectedPeriod",
                        "positions"
                )))
                .andExpect(jsonPath("$.text.format.schema.properties.title.type[*]").value(containsInAnyOrder(
                        "string",
                        "null"
                )))
                .andExpect(jsonPath("$.text.format.schema.properties.positions.items.properties.skills.items.required[*]")
                        .value(containsInAnyOrder("skillName", "importance")))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(Map.of("output_text", briefJson)),
                        MediaType.APPLICATION_JSON
                ));

        AiBriefResult result = generator.generate(AiBriefGenerateRequest.from("프로젝트 원본 입력"));

        assertThat(result.getTitle()).isEqualTo("AI가 정리한 프로젝트 제목");
        assertThat(result.getDescription()).isEqualTo("백엔드와 프론트엔드 개발자를 찾는 프로젝트입니다.");
        assertThat(result.getTotalBudgetMin()).isEqualTo(5_000_000L);
        assertThat(result.getTotalBudgetMax()).isEqualTo(8_000_000L);
        assertThat(result.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(result.getWorkPlace()).isEqualTo("판교");
        assertThat(result.getExpectedPeriod()).isEqualTo(6L);
        assertThat(result.getPositions()).hasSize(1);
        assertThat(result.getPositions().get(0).getPositionName()).isEqualTo("백엔드 개발자");
        assertThat(result.getPositions().get(0).getSkills()).hasSize(2);
        assertThat(result.getPositions().get(0).getSkills().get(0).getSkillName()).isEqualTo("Java");
        assertThat(result.getPositions().get(0).getSkills().get(0).getImportance())
                .isEqualTo(ProposalPositionSkillImportance.ESSENTIAL);
    }

    @Test
    @DisplayName("응답 본문이 없으면 AI 브리프 생성 예외가 발생한다")
    void failWhenResponseHasNoTextBody() throws JsonProcessingException {
        server.expect(requestTo(API_URL))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(Map.of("output", List.of())),
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> generator.generate(AiBriefGenerateRequest.from("원본 입력")))
                .isInstanceOf(AiBriefGenerationException.class)
                .hasMessage("AI 브리프 응답에서 본문을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("지원하지 않는 근무 형태가 오면 파싱에 실패한다")
    void failWhenWorkTypeIsUnsupported() throws JsonProcessingException {
        String briefJson = objectMapper.writeValueAsString(Map.of(
                "title", "제목",
                "description", "설명",
                "workType", "OFFICE",
                "positions", List.of()
        ));

        server.expect(requestTo(API_URL))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(Map.of("output_text", briefJson)),
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> generator.generate(AiBriefGenerateRequest.from("원본 입력")))
                .isInstanceOf(AiBriefGenerationException.class)
                .hasMessage("지원하지 않는 근무 형태입니다. value=OFFICE");
    }

    @Test
    @DisplayName("OpenAI 호출이 실패하면 AI 브리프 생성 예외로 감싼다")
    void failWhenOpenAiCallFails() {
        server.expect(requestTo(API_URL))
                .andExpect(method(POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> generator.generate(AiBriefGenerateRequest.from("원본 입력")))
                .isInstanceOf(AiBriefGenerationException.class)
                .hasMessage("OpenAI AI 브리프 호출에 실패했습니다.");
    }
}
