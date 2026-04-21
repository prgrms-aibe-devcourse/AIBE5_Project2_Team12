package com.generic4.itda.service.recommend.scoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.generic4.itda.config.ai.AiEmbeddingProperties;
import com.generic4.itda.exception.QueryEmbeddingGenerationException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "ai.embedding", name = "enabled", havingValue = "true")
public class OpenAiQueryEmbeddingGenerator implements QueryEmbeddingGenerator {

    private final RestClient restClient;
    private final AiEmbeddingProperties properties;

    @Autowired
    public OpenAiQueryEmbeddingGenerator(RestClient.Builder restClientBuilder, AiEmbeddingProperties properties) {
        Assert.hasText(properties.getApiKey(), "임베딩 API 키는 필수값입니다.");
        Assert.hasText(properties.getApiUrl(), "임베딩 API URL은 필수값입니다.");
        Assert.hasText(properties.getModel(), "임베딩 모델명은 필수값입니다.");

        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    // 테스트 전용: RestClient를 직접 주입할 수 있는 패키지-private 생성자
    OpenAiQueryEmbeddingGenerator(RestClient restClient, AiEmbeddingProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public List<Double> generate(String queryText) {
        Assert.hasText(queryText, "queryText는 비어있을 수 없습니다.");

        try {
            JsonNode responseBody = restClient.post()
                    .uri(properties.getApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                    .body(new EmbeddingRequest(
                            properties.getModel(),
                            queryText.trim()
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            return extractEmbedding(responseBody);
        } catch (QueryEmbeddingGenerationException e) {
            throw e;
        } catch (RestClientException e) {
            throw new QueryEmbeddingGenerationException("OpenAI 임베딩 호출에 실패했습니다.", e);
        } catch (RuntimeException e) {
            throw new QueryEmbeddingGenerationException("임베딩 생성에 실패했습니다.", e);
        }
    }

    private List<Double> extractEmbedding(JsonNode responseBody) {
        if (responseBody == null || responseBody.isNull()) {
            throw new QueryEmbeddingGenerationException("임베딩 응답이 비어있습니다.");
        }

        JsonNode data = responseBody.get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            throw new QueryEmbeddingGenerationException("임베딩 응답에 data가 없습니다.");
        }

        JsonNode firstItem = data.get(0);
        JsonNode embeddingNode = firstItem.get("embedding");
        if (embeddingNode == null || !embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new QueryEmbeddingGenerationException("임베딩 응답에 embedding 값이 없습니다.");
        }

        List<Double> embedding = new ArrayList<>();
        for (JsonNode valueNode : embeddingNode) {
            if (!valueNode.isNumber()) {
                throw new QueryEmbeddingGenerationException("임베딩 응답 형식이 올바르지 않습니다.");
            }
            embedding.add(valueNode.doubleValue());
        }

        return embedding;
    }

    private record EmbeddingRequest(
            String model,
            String input
    ) {

    }
}
