package com.generic4.itda.service;

import com.generic4.itda.dto.proposal.AiInterviewGenerateRequest;
import com.generic4.itda.dto.proposal.AiInterviewResult;
import com.generic4.itda.exception.AiBriefGenerationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.brief", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledAiInterviewGenerator implements AiInterviewGenerator {

    @Override
    public AiInterviewResult generate(AiInterviewGenerateRequest request) {
        throw new AiBriefGenerationException("AI 인터뷰 생성 어댑터가 등록되지 않았습니다.");
    }
}