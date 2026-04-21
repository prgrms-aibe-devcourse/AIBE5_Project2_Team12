package com.generic4.itda.service.recommend.embedding;

import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeSkill;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class ResumeEmbeddingTextGenerator {

    public String generate(Resume resume) {
        Assert.notNull(resume, "resume은 필수입니다.");

        return """
                preferredWorkType: %s
                careerYears: %s
                introduction: %s
                skills: %s
                careerDetails:
                %s
                """.formatted(
                extractPreferredWorkType(resume),
                buildCareerYears(resume.getCareerYears()),
                normalizeText(resume.getIntroduction()),
                joinSkills(resume.getSkills()),
                joinCareerDetails(resume.getCareer())
        ).trim();
    }

    private String extractPreferredWorkType(Resume resume) {
        return resume.getPreferredWorkType() == null ? "" : resume.getPreferredWorkType().name();
    }

    private String buildCareerYears(Byte careerYears) {
        return careerYears == null ? "" : careerYears + " years";
    }

    private String joinSkills(SortedSet<ResumeSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }

        return skills.stream()
                .map(this::formatSkill)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String formatSkill(ResumeSkill resumeSkill) {
        if (resumeSkill == null || resumeSkill.getSkill() == null) {
            return "";
        }

        String skillName = normalizeText(resumeSkill.getSkill().getName());
        String proficiency = resumeSkill.getProficiency() == null
                ? ""
                : resumeSkill.getProficiency().name();

        if (skillName.isBlank()) {
            return "";
        }
        return proficiency.isBlank() ? skillName : "%s (%s)".formatted(skillName, proficiency);
    }

    private String joinCareerDetails(CareerPayload careerPayload) {
        if (careerPayload == null || careerPayload.getItems() == null || careerPayload.getItems().isEmpty()) {
            return "";
        }

        return careerPayload.getItems().stream()
                .filter(Objects::nonNull)
                .map(this::formatCareerItem)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String formatCareerItem(CareerItemPayload item) {
        String companyName = normalizeText(item.getCompanyName());
        String position = normalizeText(item.getPosition());
        String employmentType = item.getEmploymentType() == null
                ? ""
                : normalizeText(item.getEmploymentType().getDescription());
        String period = buildPeriod(item);
        String summary = normalizeText(item.getSummary());
        String techStack = joinTechStack(item.getTechStack());

        return "- company: %s | position: %s | employmentType: %s | period: %s | summary: %s | techStack: %s"
                .formatted(companyName, position, employmentType, period, summary, techStack);
    }

    private String buildPeriod(CareerItemPayload item) {
        String start = normalizeText(item.getStartYearMonth());
        String end = Boolean.TRUE.equals(item.getCurrentlyWorking())
                ? "present"
                : normalizeText(item.getEndYearMonth());

        if (start.isBlank() && end.isBlank()) {
            return "";
        }
        if (start.isBlank()) {
            return end;
        }
        if (end.isBlank()) {
            return start;
        }
        return start + "~" + end;
    }

    private String joinTechStack(List<String> techStack) {
        if (techStack == null || techStack.isEmpty()) {
            return "";
        }

        return techStack.stream()
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}