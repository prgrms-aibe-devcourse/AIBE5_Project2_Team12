package com.generic4.itda.service;

import com.generic4.itda.dto.proposal.AiBriefGenerateRequest;
import com.generic4.itda.dto.proposal.AiBriefResult;

public interface AiBriefGenerator {

    AiBriefResult generate(AiBriefGenerateRequest request);
}
