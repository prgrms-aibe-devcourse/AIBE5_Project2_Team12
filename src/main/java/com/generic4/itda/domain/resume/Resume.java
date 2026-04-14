package com.generic4.itda.domain.resume;

import com.generic4.itda.domain.file.StoredFile;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.shared.BaseEntity;
import com.generic4.itda.domain.skill.Skill;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, updatable = false, referencedColumnName = "id")
    private Member member;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String introduction;

    private Byte careerYears;

    @Lob
    @Column(columnDefinition = "TEXT")
    @Convert(converter = CareerPayloadJsonConverter.class)
    private CareerPayload career;

    @Enumerated(EnumType.STRING)
    private WorkType preferredWorkType;

    private boolean publiclyVisible;

    private boolean aiMatchingEnabled;

    @Enumerated(EnumType.STRING)
    private ResumeWritingStatus writingStatus;

    @Enumerated(EnumType.STRING)
    private ResumeStatus status;

    private String portfolioUrl;

    @OneToMany(mappedBy = "resume", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private final List<ResumeAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "resume", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("proficiency desc")
    private final List<ResumeSkill> skills = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Resume(Member member, String introduction, Byte careerYears, CareerPayload career,
            WorkType preferredWorkType, Boolean publiclyVisible, Boolean aiMatchingEnabled,
            ResumeWritingStatus writingStatus, ResumeStatus status, String portfolioUrl) {
        Assert.notNull(member, "회원은 필수값입니다.");
        Assert.hasText(introduction, "자기소개는 필수값입니다.");
        Assert.notNull(careerYears, "경력 연차는 필수값입니다.");
        Assert.isTrue(careerYears >= 0, "경력 연차는 음수일 수 없습니다.");
        Assert.notNull(career, "경력은 필수값입니다.");
        Assert.notNull(writingStatus, "작성 상태는 필수 입력값입니다.");
        String normalizedPortfolioUrl = normalizePortfolioUrl(portfolioUrl);

        this.member = member;
        this.introduction = introduction;
        this.careerYears = careerYears;
        this.career = career;
        this.preferredWorkType = preferredWorkType == null ? WorkType.SITE : preferredWorkType;
        this.publiclyVisible = publiclyVisible == null || publiclyVisible;
        this.aiMatchingEnabled = aiMatchingEnabled == null || aiMatchingEnabled;
        this.writingStatus = writingStatus;
        this.status = status == null ? ResumeStatus.ACTIVE : status;
        this.portfolioUrl = normalizedPortfolioUrl;
    }

    public static Resume create(
            Member member,
            String introduction,
            Byte careerYears,
            CareerPayload career,
            WorkType preferredWorkType,
            ResumeWritingStatus writingStatus,
            String portfolioUrl
    ) {
        return Resume.builder()
                .member(member)
                .introduction(introduction)
                .careerYears(careerYears)
                .career(career)
                .preferredWorkType(preferredWorkType)
                .publiclyVisible(true)
                .aiMatchingEnabled(true)
                .writingStatus(writingStatus)
                .status(ResumeStatus.ACTIVE)
                .portfolioUrl(portfolioUrl)
                .build();
    }

    public void update(
            String introduction,
            Byte careerYears,
            CareerPayload career,
            WorkType workType,
            ResumeWritingStatus writingStatus,
            String portfolioUrl
    ) {
        Assert.hasText(introduction, "자기소개는 필수값입니다.");
        Assert.notNull(careerYears, "경력 연차는 필수값입니다.");
        Assert.isTrue(careerYears >= 0, "경력 연차는 음수일 수 없습니다.");
        Assert.notNull(career, "경력은 필수값입니다.");
        Assert.notNull(writingStatus, "작성 상태는 필수값입니다.");
        String normalizedPortfolioUrl = normalizePortfolioUrl(portfolioUrl);

        this.introduction = introduction;
        this.careerYears = careerYears;
        this.career = career;
        this.preferredWorkType = workType == null ? WorkType.SITE : workType;
        this.writingStatus = writingStatus;
        this.portfolioUrl = normalizedPortfolioUrl;
    }

    public void togglePubliclyVisible() {
        this.publiclyVisible = !this.publiclyVisible;
    }

    public void toggleAiMatchingEnabled() {
        this.aiMatchingEnabled = !this.aiMatchingEnabled;
    }

    public void delete() {
        this.status = ResumeStatus.INACTIVE;
    }

    public void restore() {
        this.status = ResumeStatus.ACTIVE;
    }

    public void addFile(StoredFile file) {
        Assert.notNull(file, "첨부 파일은 필수값입니다.");
        Assert.state(this.attachments.size() <= 10, "첨부파일은 최대 10개까지 등록할 수 있습니다.");

        int order = this.attachments.size();
        ResumeAttachment attachment = ResumeAttachment.create(this, file, order);
        this.attachments.add(attachment);
    }

    public void removeFile(StoredFile file) {
        Assert.notNull(file, "삭제할 첨부 파일은 필수값입니다.");

        boolean isRemoved = this.attachments.removeIf(attachment -> attachment.getFile().equals(file));
        if (isRemoved) {
            for (int i = 0; i < attachments.size(); i++) {
                attachments.get(i).changeDisplayOrder(i);
            }
        }
    }

    public void addSkill(Skill skill, Proficiency proficiency) {
        Assert.notNull(skill, "스킬은 필수 입력값입니다.");
        Assert.notNull(proficiency, "숙련도는 필수 입력값입니다.");

        ResumeSkill resumeSkill = ResumeSkill.create(this, skill, proficiency);
        this.skills.add(resumeSkill);
    }

    public void removeSkill(Skill skill) {
        Assert.notNull(skill, "스킬은 필수 입력값입니다.");
        this.skills.removeIf(resumeSkill -> resumeSkill.getSkill().equals(skill));
    }

    public void updateSkill(Skill skill, Proficiency proficiency) {
        Assert.notNull(skill, "스킬은 필수 입력값입니다.");
        Assert.notNull(proficiency, "숙련도는 필수 입력값입니다.");

        ResumeSkill storedSkill = this.skills.stream()
                .filter(resumeSkill -> resumeSkill.getSkill().equals(skill))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기존에 없는 스킬은 업데이트 할 수 없습니다."));

        storedSkill.update(skill, proficiency);
    }

    private String normalizePortfolioUrl(String portfolioUrl) {
        if (!StringUtils.hasText(portfolioUrl)) {
            return null;
        }

        String trimmedPortfolioUrl = portfolioUrl.trim();
        Assert.isTrue(isValidUrl(trimmedPortfolioUrl), "포트폴리오 URL은 유효한 URL형식이어야 합니다.");
        return trimmedPortfolioUrl;
    }

    private boolean isValidUrl(String url) {
        try {
            URI.create(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
