package com.generic4.itda.dto.recommend;

import java.util.List;

public record RecommendationResumeCareerItem(
        String companyName,
        String position,
        String employmentTypeLabel,
        String periodLabel,
        String summary,
        List<String> techStack
) {}

