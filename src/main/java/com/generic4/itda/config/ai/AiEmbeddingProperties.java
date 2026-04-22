package com.generic4.itda.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.embedding")
public class AiEmbeddingProperties {

    private static final String STUB_MODEL_SUFFIX = ":stub";

    private boolean enabled = false;
    private String apiUrl = "https://api.openai.com/v1/embeddings";
    private String apiKey;
    private String model = "text-embedding-3-small";

    private Integer timeoutMillis = 5000;

    public String resolveEmbeddingModel() {
        String normalizedModel = model == null ? null : model.trim();
        Assert.hasText(normalizedModel, "임베딩 모델명은 필수값입니다.");

        if (enabled) {
            return normalizedModel;
        }
        if (normalizedModel.endsWith(STUB_MODEL_SUFFIX)) {
            return normalizedModel;
        }

        int maxBaseLength = 100 - STUB_MODEL_SUFFIX.length();
        if (normalizedModel.length() > maxBaseLength) {
            normalizedModel = normalizedModel.substring(0, maxBaseLength);
        }
        return normalizedModel + STUB_MODEL_SUFFIX;
    }
}
