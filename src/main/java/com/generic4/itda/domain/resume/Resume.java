package com.generic4.itda.domain.resume;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.shared.BaseEntity;
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
import jakarta.persistence.OneToOne;
import java.net.URI;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = false)
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

        this.member = member;
        this.introduction = introduction;
        this.careerYears = careerYears;
        this.career = career;
        this.preferredWorkType = preferredWorkType == null ? WorkType.SITE : preferredWorkType;
        this.publiclyVisible = publiclyVisible == null || publiclyVisible;
        this.aiMatchingEnabled = aiMatchingEnabled == null || aiMatchingEnabled;
        this.writingStatus = writingStatus;
        this.status = status == null ? ResumeStatus.ACTIVE : status;
        if (StringUtils.hasText(portfolioUrl)) {
            portfolioUrl = portfolioUrl.trim();
            Assert.isTrue(isValidUrl(portfolioUrl), "포트폴리오 URL은 유효한 URL형식이어야 합니다.");
        }
        this.portfolioUrl = portfolioUrl;
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

        this.introduction = introduction;
        this.careerYears = careerYears;
        this.career = career;
        this.preferredWorkType = workType == null ? WorkType.SITE : workType;
        this.writingStatus = writingStatus;

        if (StringUtils.hasText(portfolioUrl)) {
            portfolioUrl = portfolioUrl.trim();
            Assert.isTrue(isValidUrl(portfolioUrl), "포트폴리오 URL은 유효한 URL형식이어야 합니다.");
        }
        this.portfolioUrl = portfolioUrl;
    }

    public void togglePubliclyVisible() {
        this.publiclyVisible = !this.publiclyVisible;
    }

    public void toggleAiMatchingEnabled() {
        this.aiMatchingEnabled = !this.aiMatchingEnabled;
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
