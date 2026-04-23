package com.generic4.itda.dto.profile;

public record ProfileClientBodyViewModel(
        String displayName,
        String userTypeLabel,
        String memo,
        ProfileProjectSummaryViewModel project
) {
}
