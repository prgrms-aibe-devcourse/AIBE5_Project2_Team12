package com.generic4.itda.service;

import com.generic4.itda.dto.proposal.AiBriefGenerateRequest;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.exception.AiBriefGenerationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.brief", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledAiBriefGenerator implements AiBriefGenerator {

    @Override
    public AiBriefResult generate(AiBriefGenerateRequest request) {
        throw new AiBriefGenerationException("AI 브리프 생성 어댑터가 등록되지 않았습니다.");
    }
}
