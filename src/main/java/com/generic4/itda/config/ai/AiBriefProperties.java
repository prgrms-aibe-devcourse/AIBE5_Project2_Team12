package com.generic4.itda.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.brief")
public class AiBriefProperties {

    private boolean enabled = false;

    private String apiUrl = "https://api.openai.com/v1/responses";

    private String apiKey;

    private String model = "gpt-4.1-mini";

    private Integer maxOutputTokens = 2_000;
}
