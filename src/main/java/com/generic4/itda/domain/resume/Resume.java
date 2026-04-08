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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.Assert;

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

    @Builder
    private Resume(Member member, String introduction, Byte careerYears, CareerPayload career,
            WorkType preferredWorkType, boolean publiclyVisible, boolean aiMatchingEnabled) {
        Assert.notNull(member, "회원은 필수값입니다.");
        Assert.hasText(introduction, "자기소개는 필수값입니다.");
        Assert.notNull(careerYears, "경력 연차는 필수값입니다.");
        Assert.isTrue(careerYears >= 0, "경력 연차는 음수일 수 없습니다.");
        Assert.notNull(career, "경력은 필수값입니다.");

        this.member = member;
        this.introduction = introduction;
        this.careerYears = careerYears;
        this.career = career;
        this.preferredWorkType = preferredWorkType == null ? WorkType.SITE : preferredWorkType;
        this.publiclyVisible = publiclyVisible;
        this.aiMatchingEnabled = aiMatchingEnabled;
    }
}
