package com.generic4.itda.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.embedding")
public class AiEmbeddingProperties {

    private boolean enabled = false;
    private String apiUrl = "https://api.openai.com/v1/embeddings";
    private String apiKey;
    private String model = "text-embedding-3-small";

    private Integer timeoutMillis = 5000;
}
