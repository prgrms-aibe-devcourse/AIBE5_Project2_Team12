package com.generic4.itda.service;

import com.generic4.itda.dto.proposal.AiInterviewGenerateRequest;
import com.generic4.itda.dto.proposal.AiInterviewResult;

public interface AiInterviewGenerator {

    AiInterviewResult generate(AiInterviewGenerateRequest request);
}