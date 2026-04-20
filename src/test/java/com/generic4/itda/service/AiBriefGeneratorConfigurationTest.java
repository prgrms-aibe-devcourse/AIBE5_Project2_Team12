package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generic4.itda.config.ai.AiBriefProperties;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.repository.ProposalAiInterviewMessageRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

class AiBriefGeneratorConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("AI 브리프가 비활성화되면 fallback 생성기가 등록되고 서비스가 정상 생성된다")
    void registerDisabledGeneratorWhenAiBriefIsDisabled() {
        contextRunner
                .withPropertyValues("ai.brief.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(AiBriefGenerator.class);
                    assertThat(context).hasSingleBean(DisabledAiBriefGenerator.class);
                    assertThat(context).doesNotHaveBean(OpenAiAiBriefGenerator.class);
                    assertThat(context).doesNotHaveBean(AiInterviewGenerator.class);
                    assertThat(context).doesNotHaveBean(OpenAiAiInterviewGenerator.class);
                    assertThat(context).hasSingleBean(ProposalAiBriefService.class);
                });
    }

    @Test
    @DisplayName("AI 브리프가 활성화되고 API 키가 있으면 OpenAI 생성기가 등록된다")
    void registerOpenAiGeneratorWhenAiBriefIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "ai.brief.enabled=true",
                        "ai.brief.api-key=test-api-key",
                        "ai.brief.model=gpt-5-mini"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AiBriefGenerator.class);
                    assertThat(context).hasSingleBean(OpenAiAiBriefGenerator.class);
                    assertThat(context).hasSingleBean(AiInterviewGenerator.class);
                    assertThat(context).hasSingleBean(OpenAiAiInterviewGenerator.class);
                    assertThat(context).doesNotHaveBean(DisabledAiBriefGenerator.class);
                    assertThat(context).hasSingleBean(ProposalAiBriefService.class);
                });
    }

    @Test
    @DisplayName("AI 브리프가 활성화되었는데 API 키가 없으면 컨텍스트 생성에 실패한다")
    void failWhenApiKeyIsMissingInEnabledMode() {
        contextRunner
                .withPropertyValues("ai.brief.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("AI 브리프 API 키는 필수값입니다.");
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiBriefProperties.class)
    @Import({
            DisabledAiBriefGenerator.class,
            OpenAiAiBriefGenerator.class,
            OpenAiAiInterviewGenerator.class,
            AiBriefProposalMapper.class,
            ProposalAiBriefService.class
    })
    static class TestConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ProposalRepository proposalRepository() {
            return mock(ProposalRepository.class);
        }

        @Bean
        ProposalAiInterviewMessageRepository proposalAiInterviewMessageRepository() {
            return mock(ProposalAiInterviewMessageRepository.class);
        }

        @Bean
        PositionRepository positionRepository() {
            return mock(PositionRepository.class);
        }

        @Bean
        SkillRepository skillRepository() {
            return mock(SkillRepository.class);
        }
    }
}