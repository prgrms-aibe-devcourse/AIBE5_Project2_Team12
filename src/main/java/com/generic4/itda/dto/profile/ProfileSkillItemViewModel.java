package com.generic4.itda.dto.profile;

public record ProfileSkillItemViewModel(
        String name,
        String proficiencyLabel,
        String proficiencyCode
) {
    public ProfileSkillItemViewModel(String name, String proficiencyLabel) {
        this(name, proficiencyLabel, null);
    }
}
