package com.generic4.itda.domain.resume;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.file.StoredFile;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.skill.Skill;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ResumeTest {

    @DisplayName("유효한 입력이 주어지면 이력서를 생성한다")
    @ParameterizedTest
    @ValueSource(bytes = {0, 3})
    void createWithValidInputs(byte careerYears) {
        Member member = createMember();

        Resume resume = createResume(
                member,
                "백엔드 개발자입니다.",
                careerYears,
                createCareerPayload()
        );

        assertThat(resume.getMember()).isEqualTo(member);
        assertThat(resume.getIntroduction()).isEqualTo("백엔드 개발자입니다.");
        assertThat(resume.getCareerYears()).isEqualTo(careerYears);
        assertThat(resume.getCareer().getItems()).hasSize(1);
        assertThat(resume.getCareer().getItems().get(0).getCompanyName()).isEqualTo("Generic4");
        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.HYBRID);
        assertThat(resume.isPubliclyVisible()).isTrue();
        assertThat(resume.isAiMatchingEnabled()).isTrue();
    }

    @DisplayName("선택 입력이 없으면 기본값으로 이력서를 생성한다.")
    @Test
    void createWithDefaultOptionsWhenOptionalInputsAreNull() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                null,
                ResumeWritingStatus.WRITING,
                null
        );

        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.SITE);
        assertThat(resume.isPubliclyVisible()).isTrue();
        assertThat(resume.isAiMatchingEnabled()).isTrue();
        assertThat(resume.getWritingStatus()).isEqualTo(ResumeWritingStatus.WRITING);
        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.ACTIVE);
    }

    @DisplayName("회원이 누락되면 이력서 생성에 실패한다")
    @Test
    void failWhenMemberIsNull() {
        assertThatThrownBy(() -> createResume(
                null,
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("회원은 필수값입니다.");
    }

    @DisplayName("자기소개가 누락되면 이력서 생성에 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenIntroductionIsMissing(String introduction) {
        assertThatThrownBy(() -> createResume(
                createMember(),
                introduction,
                (byte) 3,
                createCareerPayload()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기소개는 필수값입니다.");
    }

    @DisplayName("경력 정보가 누락되면 이력서 생성에 실패한다")
    @Test
    void failWhenCareerIsMissing() {
        assertThatThrownBy(() -> createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력은 필수값입니다.");
    }

    @DisplayName("경력 연차가 누락되면 이력서 생성에 실패한다")
    @Test
    void failWhenCareerYearsIsNull() {
        assertThatThrownBy(() -> createResume(
                createMember(),
                "백엔드 개발자입니다.",
                null,
                createCareerPayload()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력 연차는 필수값입니다.");
    }

    @DisplayName("경력 연차가 음수이면 이력서 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(bytes = {-1, -10})
    void failWhenCareerYearsIsNegative(byte careerYears) {
        assertThatThrownBy(() -> createResume(
                createMember(),
                "백엔드 개발자입니다.",
                careerYears,
                createCareerPayload()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력 연차는 음수일 수 없습니다.");
    }

    @DisplayName("유효한 입력이 주어지면 이력서를 수정한다")
    @Test
    void updateWithValidInputs() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        CareerPayload updatedCareer = createCareerPayload("OpenAI", "대규모 트래픽 환경에서 API를 운영했습니다.");

        resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 5,
                updatedCareer,
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                "https://github.com/kim"
        );

        assertThat(resume.getIntroduction()).isEqualTo("플랫폼 백엔드 개발자입니다.");
        assertThat(resume.getCareerYears()).isEqualTo((byte) 5);
        assertThat(resume.getCareer()).isEqualTo(updatedCareer);
        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.REMOTE);
        assertThat(resume.getWritingStatus()).isEqualTo(ResumeWritingStatus.DONE);
    }

    @DisplayName("수정 시 선호 근무 형태가 누락되면 SITE를 기본값으로 사용한다")
    @Test
    void updateWithDefaultWorkTypeWhenNull() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                createCareerPayload("Generic4", "배치 시스템을 개선했습니다."),
                null,
                ResumeWritingStatus.DONE,
                "https://github.com/kim"
        );

        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.SITE);
    }

    @DisplayName("수정 시 자기소개가 누락되면 실패한다")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void failWhenUpdateIntroductionIsMissing(String introduction) {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        assertThatThrownBy(() -> resume.update(
                introduction,
                (byte) 4,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://github.com/kim"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기소개는 필수값입니다.");
    }

    @DisplayName("수정 시 경력 정보가 누락되면 실패한다")
    @Test
    void failWhenUpdateCareerIsMissing() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        assertThatThrownBy(() -> resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                null,
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://github.com/kim"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력은 필수값입니다.");
    }

    @DisplayName("수정 시 경력 연차가 누락되면 실패한다")
    @Test
    void failWhenUpdateCareerYearsIsNull() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        assertThatThrownBy(() -> resume.update(
                "플랫폼 백엔드 개발자입니다.",
                null,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://github.com/kim"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력 연차는 필수값입니다.");
    }

    @DisplayName("수정 시 경력 연차가 음수이면 실패한다")
    @ParameterizedTest
    @ValueSource(bytes = {-1, -10})
    void failWhenUpdateCareerYearsIsNegative(byte careerYears) {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        assertThatThrownBy(() -> resume.update(
                "플랫폼 백엔드 개발자입니다.",
                careerYears,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://github.com/kim"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력 연차는 음수일 수 없습니다.");
    }

    @DisplayName("공개 여부 토글 시 상태가 반전된다")
    @Test
    void togglePubliclyVisible() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        resume.togglePubliclyVisible();
        assertThat(resume.isPubliclyVisible()).isFalse();

        resume.togglePubliclyVisible();
        assertThat(resume.isPubliclyVisible()).isTrue();
    }

    @DisplayName("AI 매칭 허용 여부 토글 시 상태가 반전된다")
    @Test
    void toggleAiMatchingEnabled() {
        Resume resume = createResume(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload()
        );

        resume.toggleAiMatchingEnabled();
        assertThat(resume.isAiMatchingEnabled()).isFalse();

        resume.toggleAiMatchingEnabled();
        assertThat(resume.isAiMatchingEnabled()).isTrue();
    }

    @DisplayName("유효한 포트폴리오 URL이 주어지면 이력서를 생성한다")
    @Test
    void createWithValidPortfolioUrl() {
        Resume resume = Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://github.com/user"
        );

        assertThat(resume.getPortfolioUrl()).isEqualTo("https://github.com/user");
    }

    @DisplayName("포트폴리오 URL에 앞뒤 공백이 있으면 제거하여 저장한다")
    @Test
    void createWithPortfolioUrlTrimsWhitespace() {
        Resume resume = Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "  https://github.com/user  "
        );

        assertThat(resume.getPortfolioUrl()).isEqualTo("https://github.com/user");
    }

    @DisplayName("포트폴리오 URL이 null이면 저장하지 않는다")
    @Test
    void createWithNullPortfolioUrl() {
        Resume resume = Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                null
        );

        assertThat(resume.getPortfolioUrl()).isNull();
    }

    @DisplayName("포트폴리오 URL이 공백이면 저장하지 않는다")
    @Test
    void createWithBlankPortfolioUrl() {
        Resume resume = Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "   "
        );

        assertThat(resume.getPortfolioUrl()).isNull();
    }

    @DisplayName("유효하지 않은 포트폴리오 URL이면 이력서 생성에 실패한다")
    @Test
    void failWhenPortfolioUrlIsInvalid() {
        assertThatThrownBy(() -> Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "not a valid url"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포트폴리오 URL은 유효한 URL형식이어야 합니다.");
    }

    @DisplayName("생성 시 작성 상태가 누락되면 실패한다")
    @Test
    void failWhenWritingStatusIsNullOnCreate() {
        assertThatThrownBy(() -> Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("작성 상태는 필수 입력값입니다.");
    }

    @DisplayName("임시 저장 상태로 이력서를 생성한다")
    @Test
    void createWithDraftWritingStatus() {
        Resume resume = Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.WRITING,
                null
        );

        assertThat(resume.getWritingStatus()).isEqualTo(ResumeWritingStatus.WRITING);
        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.ACTIVE);
    }

    @DisplayName("수정 시 작성 상태가 누락되면 실패한다")
    @Test
    void failWhenWritingStatusIsNullOnUpdate() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        assertThatThrownBy(() -> resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                createCareerPayload(),
                WorkType.HYBRID,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("작성 상태는 필수값입니다.");
    }

    @DisplayName("임시 저장 상태로 이력서를 수정한다")
    @Test
    void updateWithDraftWritingStatus() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                createCareerPayload("Generic4", "배치 시스템을 개선했습니다."),
                WorkType.HYBRID,
                ResumeWritingStatus.WRITING,
                null
        );

        assertThat(resume.getWritingStatus()).isEqualTo(ResumeWritingStatus.WRITING);
    }

    @DisplayName("이력서를 삭제하면 상태가 INACTIVE로 변경된다")
    @Test
    void deleteResume() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        resume.delete();

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.INACTIVE);
    }

    @DisplayName("삭제된 이력서를 복원하면 상태가 ACTIVE로 변경된다")
    @Test
    void restoreResume() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        resume.delete();

        resume.restore();

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.ACTIVE);
    }

    @DisplayName("수정 시 유효한 포트폴리오 URL이면 저장된다")
    @Test
    void updateWithValidPortfolioUrl() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "https://github.com/user"
        );

        assertThat(resume.getPortfolioUrl()).isEqualTo("https://github.com/user");
    }

    @DisplayName("수정 시 포트폴리오 URL에 앞뒤 공백이 있으면 제거하여 저장한다")
    @Test
    void updateWithPortfolioUrlTrimsWhitespace() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "  https://github.com/user  "
        );

        assertThat(resume.getPortfolioUrl()).isEqualTo("https://github.com/user");
    }

    @DisplayName("수정 시 포트폴리오 URL이 공백이면 기존 값을 제거한다")
    @Test
    void updateWithBlankPortfolioUrlClearsExistingValue() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "   "
        );

        assertThat(resume.getPortfolioUrl()).isNull();
    }

    @DisplayName("수정 시 유효하지 않은 포트폴리오 URL이면 실패한다")
    @Test
    void failWhenUpdatePortfolioUrlIsInvalid() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        assertThatThrownBy(() -> resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.DONE,
                "not a valid url"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포트폴리오 URL은 유효한 URL형식이어야 합니다.");
    }

    @DisplayName("수정 시 포트폴리오 URL 검증에 실패하면 기존 상태를 유지한다")
    @Test
    void failWhenUpdatePortfolioUrlIsInvalidPreservesState() {
        CareerPayload originalCareer = createCareerPayload();
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, originalCareer);

        assertThatThrownBy(() -> resume.update(
                "플랫폼 백엔드 개발자입니다.",
                (byte) 4,
                createCareerPayload("OpenAI", "대규모 트래픽 환경에서 API를 운영했습니다."),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                "not a valid url"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포트폴리오 URL은 유효한 URL형식이어야 합니다.");

        assertThat(resume.getIntroduction()).isEqualTo("백엔드 개발자입니다.");
        assertThat(resume.getCareerYears()).isEqualTo((byte) 3);
        assertThat(resume.getCareer()).isSameAs(originalCareer);
        assertThat(resume.getPreferredWorkType()).isEqualTo(WorkType.HYBRID);
        assertThat(resume.getWritingStatus()).isEqualTo(ResumeWritingStatus.WRITING);
        assertThat(resume.getPortfolioUrl()).isEqualTo("https://github.com/user");
    }

    @DisplayName("파일을 삭제하면 첨부파일 목록에서 제거된다")
    @Test
    void removeFileDeletesAttachment() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        StoredFile file = createStoredFile("resume.pdf");
        resume.addFile(file);

        resume.removeFile(file);

        assertThat(resume.getAttachments()).isEmpty();
    }

    @DisplayName("존재하지 않는 파일을 삭제해도 첨부파일 목록이 변하지 않는다")
    @Test
    void removeNonExistentFileDoesNotChangeAttachments() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        StoredFile file = createStoredFile("resume.pdf");
        StoredFile otherFile = createStoredFile("other.pdf");
        resume.addFile(file);

        resume.removeFile(otherFile);

        assertThat(resume.getAttachments()).hasSize(1);
        assertThat(resume.getAttachments().get(0).getFile()).isEqualTo(file);
    }

    @DisplayName("null 파일을 추가하면 실패한다")
    @Test
    void failWhenAddFileIsNull() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        assertThatThrownBy(() -> resume.addFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("첨부 파일은 필수값입니다.");
    }

    @DisplayName("첨부파일이 10개이상이면 추가에 실패한다")
    @Test
    void failWhenAttachmentsExceedMaxLimit() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        for (int i = 0; i <= 10; i++) {
            resume.addFile(createStoredFile("file" + i + ".pdf"));
        }

        assertThatThrownBy(() -> resume.addFile(createStoredFile("extra.pdf")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("첨부파일은 최대 10개까지 등록할 수 있습니다.");
    }

    @DisplayName("null 파일을 삭제하면 실패한다")
    @Test
    void failWhenRemoveFileIsNull() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        assertThatThrownBy(() -> resume.removeFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("삭제할 첨부 파일은 필수값입니다.");
    }

    @DisplayName("스킬을 추가하면 스킬 목록에 등록된다")
    @Test
    void addSkillAppendsToSkillList() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        Skill skill = Skill.create("Java", "백엔드 언어");

        resume.addSkill(skill, Proficiency.ADVANCED);

        assertThat(resume.getSkills()).hasSize(1);
        assertThat(resume.getSkills().get(0).getSkill()).isEqualTo(skill);
        assertThat(resume.getSkills().get(0).getProficiency()).isEqualTo(Proficiency.ADVANCED);
    }

    @DisplayName("스킬을 삭제하면 스킬 목록에서 제거된다")
    @Test
    void removeSkillDeletesFromSkillList() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        Skill skill = Skill.create("Java", "백엔드 언어");
        resume.addSkill(skill, Proficiency.ADVANCED);

        resume.removeSkill(skill);

        assertThat(resume.getSkills()).isEmpty();
    }

    @DisplayName("존재하지 않는 스킬을 삭제해도 스킬 목록이 변하지 않는다")
    @Test
    void removeNonExistentSkillDoesNotChangeSkillList() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        Skill skill = Skill.create("Java", "백엔드 언어");
        Skill otherSkill = Skill.create("Python", "스크립트 언어");
        resume.addSkill(skill, Proficiency.ADVANCED);

        resume.removeSkill(otherSkill);

        assertThat(resume.getSkills()).hasSize(1);
    }

    @DisplayName("스킬을 수정하면 숙련도가 변경된다")
    @Test
    void updateSkillChangesProficiency() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        Skill skill = Skill.create("Java", "백엔드 언어");
        resume.addSkill(skill, Proficiency.BEGINNER);

        resume.updateSkill(skill, Proficiency.ADVANCED);

        assertThat(resume.getSkills().get(0).getProficiency()).isEqualTo(Proficiency.ADVANCED);
    }

    @DisplayName("null 스킬을 추가하면 실패한다")
    @Test
    void failWhenAddSkillIsNull() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        assertThatThrownBy(() -> resume.addSkill(null, Proficiency.BEGINNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스킬은 필수 입력값입니다.");
    }

    @DisplayName("null 숙련도로 스킬을 추가하면 실패한다")
    @Test
    void failWhenAddProficiencyIsNull() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        Skill skill = Skill.create("Java", "백엔드 언어");

        assertThatThrownBy(() -> resume.addSkill(skill, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("숙련도는 필수 입력값입니다.");
    }

    @DisplayName("null 스킬을 삭제하면 실패한다")
    @Test
    void failWhenRemoveSkillIsNull() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        assertThatThrownBy(() -> resume.removeSkill(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스킬은 필수 입력값입니다.");
    }

    @DisplayName("존재하지 않는 스킬을 수정하면 실패한다")
    @Test
    void failWhenUpdateNonExistentSkill() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        Skill skill = Skill.create("Java", "백엔드 언어");

        assertThatThrownBy(() -> resume.updateSkill(skill, Proficiency.ADVANCED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("기존에 없는 스킬은 업데이트 할 수 없습니다.");
    }

    @DisplayName("null 스킬로 수정하면 실패한다")
    @Test
    void failWhenUpdateSkillIsNull() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());

        assertThatThrownBy(() -> resume.updateSkill(null, Proficiency.ADVANCED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스킬은 필수 입력값입니다.");
    }

    @DisplayName("null 숙련도로 스킬을 수정하면 실패한다")
    @Test
    void failWhenUpdateProficiencyIsNull() {
        Resume resume = createResume(createMember(), "백엔드 개발자입니다.", (byte) 3, createCareerPayload());
        Skill skill = Skill.create("Java", "백엔드 언어");
        resume.addSkill(skill, Proficiency.BEGINNER);

        assertThatThrownBy(() -> resume.updateSkill(skill, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("숙련도는 필수 입력값입니다.");
    }

    private static StoredFile createStoredFile(String originalName) {
        return StoredFile.create(originalName, "stored-" + originalName, "/files/" + originalName, "application/pdf",
                1024L);
    }

    private static Resume createResume(Member member, String introduction, Byte careerYears, CareerPayload career) {
        return Resume.create(member,
                introduction,
                careerYears,
                career,
                WorkType.HYBRID,
                ResumeWritingStatus.WRITING,
                "https://github.com/user"
        );
    }

    private static Resume createResume(Member member, String introduction, Byte careerYears, CareerPayload career,
            WorkType preferredWorkType, ResumeWritingStatus writingStatus, String portfolioUrl) {
        return Resume.create(
                member,
                introduction,
                careerYears,
                career,
                preferredWorkType,
                writingStatus,
                portfolioUrl
        );
    }

    private static CareerPayload createCareerPayload() {
        return createCareerPayload("Generic4", "Spring Boot 기반 API를 개발하고 운영했습니다.");
    }

    private static CareerPayload createCareerPayload(String companyName, String summary) {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName(companyName);
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setEndYearMonth(null);
        item.setCurrentlyWorking(true);
        item.setSummary(summary);
        item.setTechStack(List.of("Java", "Spring Boot", "PostgreSQL"));

        CareerPayload payload = new CareerPayload();
        payload.setItems(List.of(item));
        return payload;
    }
}
